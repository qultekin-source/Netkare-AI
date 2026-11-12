package com.example.data.ai

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.GeneratedVideoResult
import com.example.domain.model.VideoAspectRatio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

private const val TAG = "InteractionsVideoSvc"
private const val INTERACTIONS_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/interactions"
private const val MODEL_OMNI_VIDEO = "gemini-omni-1.2-flash"

class InteractionsVideoService {

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun generateVideoFromText(
        context: Context,
        prompt: String,
        aspectRatio: VideoAspectRatio,
        duration: String,
        resolution: String,
        previousInteractionId: String? = null,
        onStatusUpdate: (String) -> Unit
    ): GeneratedVideoResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException(
                "Metinden video üretimi (gemini-omni-1.2-flash Interactions API) için AI Studio Secrets panelinden geçerli bir GEMINI_API_KEY tanımlamanız gerekiyor."
            )
        }

        try {
            onStatusUpdate("Interactions API isteği gönderiliyor ($MODEL_OMNI_VIDEO)...")
            val payload = JSONObject().apply {
                put("model", MODEL_OMNI_VIDEO)
                put("input", prompt)
                put(
                    "response_format",
                    JSONObject().apply {
                        put("type", "video")
                        put("aspect_ratio", aspectRatio.apiValue)
                        put("duration", duration)
                        put("resolution", resolution)
                    }
                )
                put("store", true)
                if (!previousInteractionId.isNullOrBlank()) {
                    put("previous_interaction_id", previousInteractionId)
                }
            }

            val createRequest = Request.Builder()
                .url(INTERACTIONS_ENDPOINT)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            var currentJson = executeJsonRequest(createRequest)
            val interactionId = currentJson.optString("id").takeIf { it.isNotBlank() }
            var status = currentJson.optString("status", "")

            // Poll if the interaction runs asynchronously on the server
            var pollAttempt = 0
            while ((status == "in_progress" || status == "requires_action" || status == "pending") &&
                !interactionId.isNullOrBlank() &&
                pollAttempt < 45
            ) {
                pollAttempt++
                onStatusUpdate("Video kareleri oluşturuluyor (${aspectRatio.apiValue} • $resolution) — adım $pollAttempt...")
                delay(3000L)
                val pollRequest = Request.Builder()
                    .url("$INTERACTIONS_ENDPOINT/$interactionId")
                    .addHeader("x-goog-api-key", apiKey)
                    .get()
                    .build()
                currentJson = executeJsonRequest(pollRequest)
                status = currentJson.optString("status", "")
            }

            if (status == "failed" || status == "cancelled") {
                val errorObj = currentJson.optJSONObject("error")
                val errMsg = errorObj?.optString("message")
                    ?: currentJson.optString("status_message")
                    ?: "Video oluşturma başarısız oldu (durum: $status)."
                throw IllegalStateException(errMsg)
            }

            onStatusUpdate("Oluşturulan MP4 dosyası kaydediliyor...")
            val videoBytes = extractVideoBytes(currentJson, apiKey)
                ?: throw IllegalStateException("Interactions API yanıtında video verisi bulunamadı.")

            val outDir = File(context.filesDir, "restored").apply { mkdirs() }
            val outFile = File(outDir, "netkare_omni_${System.currentTimeMillis()}.mp4")
            FileOutputStream(outFile).use { it.write(videoBytes) }

            GeneratedVideoResult(
                filePath = outFile.absolutePath,
                prompt = prompt,
                aspectRatio = aspectRatio,
                resolution = resolution,
                duration = duration,
                interactionId = interactionId
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Interactions video generation failed", e)
            throw e
        }
    }

    private fun executeJsonRequest(request: Request): JSONObject {
        httpClient.newCall(request).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val parsedError = runCatching {
                    JSONObject(rawBody).optJSONObject("error")?.optString("message")
                }.getOrNull()
                throw IllegalStateException(
                    parsedError?.takeIf { it.isNotBlank() }
                        ?: "Interactions API HTTP ${response.code}: ${rawBody.take(240)}"
                )
            }
            return JSONObject(rawBody)
        }
    }

    private fun extractVideoBytes(json: JSONObject, apiKey: String): ByteArray? {
        // 1. Direct top-level output_video object (SDK/REST legacy representation)
        json.optJSONObject("output_video")?.let { outVid ->
            decodeOrDownloadMediaObject(outVid, apiKey)?.let { return it }
        }

        // 2. Current schema: steps[].content[]
        json.optJSONArray("steps")?.let { steps ->
            findVideoInArray(steps, apiKey)?.let { return it }
        }

        // 3. Legacy schema: outputs[]
        json.optJSONArray("outputs")?.let { outputs ->
            findVideoInArray(outputs, apiKey)?.let { return it }
        }

        // 4. Recursive search fallback across JSON tree
        return findVideoRecursively(json, apiKey)
    }

    private fun findVideoInArray(array: JSONArray, apiKey: String): ByteArray? {
        for (i in array.length() - 1 downTo 0) {
            val item = array.opt(i)
            when (item) {
                is JSONObject -> {
                    findVideoRecursively(item, apiKey)?.let { return it }
                }
            }
        }
        return null
    }

    private fun findVideoRecursively(obj: JSONObject, apiKey: String): ByteArray? {
        decodeOrDownloadMediaObject(obj, apiKey)?.let { return it }
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            when (val value = obj.opt(key)) {
                is JSONObject -> findVideoRecursively(value, apiKey)?.let { return it }
                is JSONArray -> {
                    for (i in 0 until value.length()) {
                        val elem = value.opt(i)
                        if (elem is JSONObject) {
                            findVideoRecursively(elem, apiKey)?.let { return it }
                        }
                    }
                }
            }
        }
        return null
    }

    private fun decodeOrDownloadMediaObject(obj: JSONObject, apiKey: String): ByteArray? {
        val mime = obj.optString("mime_type").ifBlank { obj.optString("mimeType") }
        val type = obj.optString("type")
        val data = obj.optString("data")
        if (data.isNotBlank() && (mime.startsWith("video") || type == "video" || data.length > 1024)) {
            val cleanBase64 = data.substringAfter("base64,", data)
            return runCatching { Base64.decode(cleanBase64, Base64.DEFAULT) }.getOrNull()
        }
        val uri = obj.optString("uri").ifBlank { obj.optString("file_uri") }
        if (uri.startsWith("http")) {
            val downloadReq = Request.Builder()
                .url(uri)
                .addHeader("x-goog-api-key", apiKey)
                .get()
                .build()
            httpClient.newCall(downloadReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    return resp.body?.bytes()
                }
            }
        }
        return null
    }
}
