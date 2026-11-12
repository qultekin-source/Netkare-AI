package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.ai.AiRestorationService
import com.example.domain.model.ChatMessage
import com.example.domain.model.ChatbotRole
import com.example.domain.model.GeminiChatModel
import com.google.firebase.ai.Chat
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "ChatViewModel"

data class ChatUiState(
    val selectedModel: GeminiChatModel = GeminiChatModel.HYDROGEN,
    val selectedRole: ChatbotRole = ChatbotRole.RESTORATION_EXPERT,
    val inputText: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isResponding: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val aiService: AiRestorationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var activeChat: Chat? = null
    private var activeModel: GeminiChatModel? = null
    private var activeRole: ChatbotRole? = null
    private var nextMessageId = 1L

    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun selectModel(model: GeminiChatModel) {
        if (_uiState.value.selectedModel == model) return
        _uiState.update { it.copy(selectedModel = model, errorMessage = null) }
        rebuildChatSessionPreservingHistory()
    }

    fun selectRole(role: ChatbotRole) {
        if (_uiState.value.selectedRole == role) return
        _uiState.update { it.copy(selectedRole = role, errorMessage = null) }
        rebuildChatSessionPreservingHistory()
    }

    fun clearConversation() {
        activeChat = null
        activeModel = null
        activeRole = null
        _uiState.update {
            it.copy(
                messages = emptyList(),
                isResponding = false,
                errorMessage = null
            )
        }
    }

    fun sendMessage(overridePrompt: String? = null) {
        val prompt = (overridePrompt ?: _uiState.value.inputText).trim()
        if (prompt.isEmpty() || _uiState.value.isResponding) return

        val currentModel = _uiState.value.selectedModel
        val currentRole = _uiState.value.selectedRole
        val chatSession = ensureChatSession(currentModel, currentRole)

        val userMsg = ChatMessage(
            id = nextMessageId++,
            isUser = true,
            text = prompt
        )
        val assistantMsgId = nextMessageId++
        val assistantPlaceholder = ChatMessage(
            id = assistantMsgId,
            isUser = false,
            text = "",
            modelName = currentModel.modelId,
            isStreaming = true
        )

        _uiState.update {
            it.copy(
                inputText = if (overridePrompt == null) "" else it.inputText,
                messages = it.messages + userMsg + assistantPlaceholder,
                isResponding = true,
                errorMessage = null
            )
        }

        viewModelScope.launch {
            val accumulated = StringBuilder()
            try {
                withTimeout(60_000L) {
                    chatSession.sendMessageStream(prompt).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            accumulated.append(piece)
                            updateAssistantMessage(assistantMsgId, accumulated.toString(), isStreaming = true)
                        }
                    }
                }
                val finalResponse = accumulated.toString().ifBlank {
                    "Model boş yanıt döndürdü."
                }
                updateAssistantMessage(assistantMsgId, finalResponse, isStreaming = false)
                _uiState.update { it.copy(isResponding = false) }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Chat stream timed out", e)
                removeOrFailAssistantMessage(
                    assistantMsgId,
                    accumulated.toString(),
                    "Yanıt 60 saniye içinde tamamlanamadı. Lütfen tekrar deneyin."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Chat generation failed", e)
                removeOrFailAssistantMessage(
                    assistantMsgId,
                    accumulated.toString(),
                    e.localizedMessage ?: "Sohbet yanıtı alınamadı."
                )
            }
        }
    }

    private fun ensureChatSession(model: GeminiChatModel, role: ChatbotRole): Chat {
        val existing = activeChat
        if (existing != null && activeModel == model && activeRole == role) {
            return existing
        }
        return rebuildChatSessionPreservingHistory()
    }

    private fun rebuildChatSessionPreservingHistory(): Chat {
        val currentState = _uiState.value
        val sdkHistory: List<Content> = activeChat?.history ?: currentState.messages
            .filter { it.text.isNotBlank() && !it.isStreaming }
            .map { msg ->
                content(role = if (msg.isUser) "user" else "model") {
                    text(msg.text)
                }
            }
        val rebuilt = aiService.createChatSession(
            chatModel = currentState.selectedModel,
            role = currentState.selectedRole,
            history = sdkHistory
        )
        activeChat = rebuilt
        activeModel = currentState.selectedModel
        activeRole = currentState.selectedRole
        return rebuilt
    }

    private fun updateAssistantMessage(id: Long, newText: String, isStreaming: Boolean) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.map { msg ->
                    if (msg.id == id) msg.copy(text = newText, isStreaming = isStreaming) else msg
                }
            )
        }
    }

    private fun removeOrFailAssistantMessage(id: Long, partialText: String, errorMsg: String) {
        _uiState.update { state ->
            val updatedList = if (partialText.isBlank()) {
                state.messages.filterNot { it.id == id }
            } else {
                state.messages.map { msg ->
                    if (msg.id == id) msg.copy(text = partialText, isStreaming = false) else msg
                }
            }
            state.copy(
                messages = updatedList,
                isResponding = false,
                errorMessage = errorMsg
            )
        }
    }

    companion object {
        fun provideFactory(aiService: AiRestorationService): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ChatViewModel(aiService)
                }
            }
    }
}
