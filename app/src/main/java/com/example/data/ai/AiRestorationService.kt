package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.domain.model.AiDiagnosticResult
import com.example.domain.model.AiGuidanceTip
import com.example.domain.model.ChatbotRole
import com.example.domain.model.EnhancementLevel
import com.example.domain.model.GeminiChatModel
import com.example.domain.model.ImageAspectRatioOption
import com.example.domain.model.PhotoRestorationPreset
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AspectRatio
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImageConfig
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.roundToInt

private const val TAG = "AiRestorationService"

class AiRestorationService {

    private val diagnosticSchema = Schema.obj(
        properties = mapOf(
            "qualityScore" to Schema.integer(
                description = "Overall current visual quality score from 1 to 100"
            ),
            "detectedIssues" to Schema.array(
                items = Schema.string(),
                description = "2 to 4 concise Turkish tags describing detected issues (e.g., Düşük Işık, Hareket Bulanıklığı, Düşük Çözünürlük, Soluk Renk)"
            ),
            "summaryTr" to Schema.string(
                description = "2-sentence Turkish technical diagnosis and restoration recommendation"
            ),
            "recommendedPreset" to Schema.enumeration(
                values = listOf("VINTAGE_RESTORE", "SHARPEN_CLARITY", "COLOR_REVIVE", "ARCHIVE_NATURAL"),
                description = "Best matching video restoration preset key"
            ),
            "recommendedPhotoPreset" to Schema.enumeration(
                values = listOf("ONE_TAP_ULTRA", "DEBLUR_FOCUS", "COLORIZE_REVIVE", "PORTRAIT_DETAIL"),
                description = "Best matching photo restoration preset key"
            ),
            "recommendedLevel" to Schema.enumeration(
                values = listOf("LOW", "MEDIUM", "HIGH"),
                description = "Recommended enhancement intensity level"
            ),
            "recommendedResolution" to Schema.enumeration(
                values = listOf("HD_720P", "FHD_1080P", "QHD_1440P", "UHD_4K"),
                description = "Recommended target upscale resolution"
            ),
            "suggestedSharpness" to Schema.double(
                description = "Recommended sharpness factor between 0.3 and 1.0"
            ),
            "suggestedDenoise" to Schema.double(
                description = "Recommended denoise strength between 0.2 and 1.0"
            ),
            "suggestedContrast" to Schema.double(
                description = "Recommended contrast boost between 0.05 and 0.45"
            ),
            "suggestedSaturation" to Schema.double(
                description = "Recommended HSL saturation adjustment between -5.0 and 40.0"
            ),
            "suggestedBrightness" to Schema.double(
                description = "Recommended brightness/lightness adjustment from -12.0 to +30.0 (positive values brighten dark media)"
            ),
            "guidanceTips" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "categoryTr" to Schema.string(
                            description = "Short Turkish category (e.g., Işık & Parlaklık, Netlik & Bulanıklık, Renk & Kontrast, Çözünürlük)"
                        ),
                        "messageTr" to Schema.string(
                            description = "Clear natural Turkish guidance sentence explaining the visual finding and recommendation (e.g., 'Bu video biraz karanlık, parlaklık artırma önerilir.')"
                        ),
                        "actionBadgeTr" to Schema.string(
                            description = "Short Turkish setting badge (e.g., 'Parlaklık +16', 'Keskinlik %80', 'Seviye: Yüksek')"
                        )
                    )
                ),
                description = "2 to 4 actionable Turkish guidance tips tailored to the uploaded media's visual lighting, blur, noise, color, and resolution"
            )
        )
    )

    private val diagnosticModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = diagnosticSchema
            }
        )
    }

    private val photoRestoreModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-nano-banana-2.1",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.IMAGE)
            }
        )
    }

    fun createChatSession(
        chatModel: GeminiChatModel,
        role: ChatbotRole,
        history: List<Content> = emptyList()
    ): Chat {
        val model = Firebase.ai.generativeModel(
            modelName = chatModel.modelId,
            systemInstruction = content { text(role.systemPromptTr) }
        )
        return model.startChat(history = history)
    }

    suspend fun generateImageFromPrompt(
        promptText: String,
        aspectRatio: ImageAspectRatioOption
    ): Bitmap {
        return try {
            val model = Firebase.ai.generativeModel(
                modelName = "gemini-nano-banana-2.1",
                generationConfig = generationConfig {
                    responseModalities = listOf(ResponseModality.IMAGE)
                    imageConfig = ImageConfig.builder()
                        .setAspectRatio(aspectRatio.toSdkAspectRatio())
                        .build()
                }
            )
            val response = withTimeout(60_000L) {
                model.generateContent(promptText)
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()

            imagePart?.image
                ?: throw IllegalStateException("Yapay zeka modeli görsel üretemedi.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Text-to-image generation failed", e)
            throw e
        }
    }

    suspend fun editImageWithPrompt(
        sourceBitmap: Bitmap,
        editInstruction: String,
        aspectRatio: ImageAspectRatioOption? = null
    ): Bitmap {
        return try {
            val inputScaled = scaleDownIfNeeded(sourceBitmap, 1280)
            val model = if (aspectRatio != null) {
                Firebase.ai.generativeModel(
                    modelName = "gemini-nano-banana-2.1",
                    generationConfig = generationConfig {
                        responseModalities = listOf(ResponseModality.IMAGE)
                        imageConfig = ImageConfig.builder()
                            .setAspectRatio(aspectRatio.toSdkAspectRatio())
                            .build()
                    }
                )
            } else {
                photoRestoreModel
            }
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        image(inputScaled)
                        text(editInstruction)
                    }
                )
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()

            imagePart?.image
                ?: throw IllegalStateException("Yapay zeka modeli düzenlenmiş görsel döndürmedi.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Prompt image editing failed", e)
            throw e
        }
    }

    suspend fun analyzeVideoFrames(
        frames: List<Bitmap>,
        width: Int,
        height: Int,
        durationMs: Long
    ): AiDiagnosticResult {
        if (frames.isEmpty()) {
            throw IllegalArgumentException("Video karesi çıkarılamadı.")
        }
        return try {
            val scaledFrames = frames.take(3).map { scaleDownIfNeeded(it, 768) }
            val response = withTimeout(60_000L) {
                diagnosticModel.generateContent(
                    content {
                        scaledFrames.forEach { frame -> image(frame) }
                        text(
                            """
                            Sen uzman bir video restorasyon ve görüntü rehberliği mühendisisin.
                            Bu kareler ${width}x${height} çözünürlüğünde, ${durationMs / 1000} saniyelik bir videodan alındı.
                            Lütfen videoyu pozlama/parlaklık (karanlık mı, aşırı pozlanmış mı), bulanıklık/netlik,
                            düşük çözünürlük, sıkıştırma gürültüsü ve renk canlılığı açısından görsel olarak incele.
                            'guidanceTips' alanında kullanıcıya doğrudan yol gösteren 2-4 net Türkçe rehber önerisi ver
                            (örneğin: "Bu video biraz karanlık, parlaklık artırma önerilir.",
                            "Karelerde hareket bulanıklığı var, keskinliği %80'e çıkarmanız önerilir.").
                            """.trimIndent()
                        )
                    }
                )
            }
            val rawJson = response.text ?: throw IllegalStateException("Yapay zeka boş yanıt döndürdü.")
            parseDiagnosticJson(rawJson)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Video diagnostic analysis failed", e)
            throw e
        }
    }

    suspend fun analyzePhoto(bitmap: Bitmap): AiDiagnosticResult {
        return try {
            val scaled = scaleDownIfNeeded(bitmap, 1024)
            val response = withTimeout(60_000L) {
                diagnosticModel.generateContent(
                    content {
                        image(scaled)
                        text(
                            """
                            Sen uzman bir fotoğraf restorasyon ve görüntü rehberliği mühendisisin.
                            Bu fotoğrafın çözünürlüğünü (${bitmap.width}x${bitmap.height}), pozlama/parlaklık durumunu,
                            netliğini, bulanıklığını, gren/çizik seviyesini ve renk dengesini görsel olarak incele.
                            'guidanceTips' alanında kullanıcıya doğrudan rehberlik eden 2-4 net Türkçe öneri üret
                            (örneğin: "Bu fotoğraf biraz karanlık görünüyor, parlaklık ve kontrast artırma önerilir.",
                            "Yüzeyde bulanıklık ve gren mevcut, Yüksek seviye bulanıklık giderme önerilir.").
                            """.trimIndent()
                        )
                    }
                )
            }
            val rawJson = response.text ?: throw IllegalStateException("Yapay zeka boş yanıt döndürdü.")
            parseDiagnosticJson(rawJson)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Photo diagnostic analysis failed", e)
            throw e
        }
    }

    suspend fun restorePhotoWithAi(
        sourceBitmap: Bitmap,
        preset: PhotoRestorationPreset,
        upscaleTargetLabel: String,
        enhancementLevel: EnhancementLevel = EnhancementLevel.MEDIUM
    ): Bitmap {
        return try {
            val inputScaled = scaleDownIfNeeded(sourceBitmap, 1280)
            val prompt = buildString {
                append(preset.promptDirective)
                append(" ")
                append(enhancementLevel.photoPromptHint)
                append(" Target resolution upscale: ")
                append(upscaleTargetLabel)
                append(". Output only the restored high-definition photograph with sharp details, natural lighting, zero artificial borders or text.")
            }
            val response = withTimeout(60_000L) {
                photoRestoreModel.generateContent(
                    content {
                        image(inputScaled)
                        text(prompt)
                    }
                )
            }
            val imagePart = response.candidates.firstOrNull()
                ?.content?.parts?.filterIsInstance<ImagePart>()
                ?.firstOrNull()

            imagePart?.image
                ?: throw IllegalStateException("Yapay zeka modeli restore edilmiş görsel döndürmedi.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI photo restoration failed", e)
            throw e
        }
    }

    private fun ImageAspectRatioOption.toSdkAspectRatio(): AspectRatio = when (this) {
        ImageAspectRatioOption.SQUARE_1_1 -> AspectRatio.SQUARE_1x1
        ImageAspectRatioOption.LANDSCAPE_16_9 -> AspectRatio.LANDSCAPE_16x9
        ImageAspectRatioOption.PORTRAIT_9_16 -> AspectRatio.PORTRAIT_9x16
        ImageAspectRatioOption.LANDSCAPE_4_3 -> AspectRatio.LANDSCAPE_4x3
        ImageAspectRatioOption.PORTRAIT_3_4 -> AspectRatio.PORTRAIT_3x4
    }

    private fun parseDiagnosticJson(rawJson: String): AiDiagnosticResult {
        val json = JSONObject(rawJson)
        val issuesArray = json.optJSONArray("detectedIssues")
        val issues = if (issuesArray != null) {
            (0 until issuesArray.length()).mapNotNull { idx ->
                issuesArray.optString(idx).takeIf { it.isNotBlank() }
            }
        } else {
            emptyList()
        }

        val tipsArray = json.optJSONArray("guidanceTips")
        val tips = if (tipsArray != null) {
            (0 until tipsArray.length()).mapNotNull { idx ->
                val obj = tipsArray.optJSONObject(idx) ?: return@mapNotNull null
                val msg = obj.optString("messageTr").trim()
                if (msg.isBlank()) return@mapNotNull null
                AiGuidanceTip(
                    categoryTr = obj.optString("categoryTr", "Ayar Önerisi").ifBlank { "Ayar Önerisi" },
                    messageTr = msg,
                    actionBadgeTr = obj.optString("actionBadgeTr", "Önerilen").ifBlank { "Önerilen" }
                )
            }
        } else {
            emptyList()
        }

        return AiDiagnosticResult(
            qualityScore = json.optInt("qualityScore", 55).coerceIn(1, 100),
            detectedIssues = issues.ifEmpty { listOf("Düşük Netlik", "Detay Kaybı") },
            summaryTr = json.optString(
                "summaryTr",
                "Görüntüde keskinlik kaybı ve sıkıştırma izleri tespit edildi. 1080p yükseltme ile netlik artırılabilir."
            ),
            recommendedPreset = json.optString("recommendedPreset", "VINTAGE_RESTORE"),
            recommendedPhotoPreset = json.optString("recommendedPhotoPreset", "ONE_TAP_ULTRA"),
            recommendedLevel = json.optString("recommendedLevel", "MEDIUM"),
            recommendedResolution = json.optString("recommendedResolution", "FHD_1080P"),
            suggestedSharpness = json.optDouble("suggestedSharpness", 0.72).toFloat().coerceIn(0.2f, 1.0f),
            suggestedDenoise = json.optDouble("suggestedDenoise", 0.70).toFloat().coerceIn(0.1f, 1.0f),
            suggestedContrast = json.optDouble("suggestedContrast", 0.22).toFloat().coerceIn(0.0f, 0.5f),
            suggestedSaturation = json.optDouble("suggestedSaturation", 18.0).toFloat().coerceIn(-15f, 45f),
            suggestedBrightness = json.optDouble("suggestedBrightness", 4.0).toFloat().coerceIn(-15f, 30f),
            guidanceTips = tips
        )
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDim: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / largest.toFloat()
        val newW = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val newH = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }
}
