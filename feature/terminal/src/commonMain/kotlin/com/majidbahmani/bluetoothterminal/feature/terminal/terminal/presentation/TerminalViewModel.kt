package com.majidbahmani.bluetoothterminal.feature.terminal.terminal.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelException
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase.DisconnectUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase.ObserveConnectionUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.model.TerminalMessage
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase.ObserveMessagesUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase.SendMessageUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Keeps the log for as long as the Terminal is in the back stack.
 *
 * Collected for the view model's whole life, not only while the screen is visible (unlike the Devices
 * scan): messages that arrive while another screen is on top must still end up in the log.
 */
internal class TerminalViewModel(
    observeConnection: ObserveConnectionUseCase,
    observeMessages: ObserveMessagesUseCase,
    private val sendMessage: SendMessageUseCase,
    private val disconnect: DisconnectUseCase,
) : ViewModel() {

    private val state = MutableStateFlow(TerminalUiState())
    val uiState: StateFlow<TerminalUiState> = state.asStateFlow()

    init {
        observeConnection()
            .onEach { connection -> state.update { it.copy(connection = connection) } }
            .launchIn(viewModelScope)
        observeMessages()
            .onEach { message -> state.update { it.copy(messages = it.messages.plusCapped(message)) } }
            .launchIn(viewModelScope)
    }

    /**
     * Emits when the connection ends on purpose (Disconnect, here or elsewhere), so the screen can close.
     * Not when the link is lost: the screen stays with the log and the "lost" banner.
     */
    val closed: Flow<Unit> = flow {
        var previous: ConnectionStatus? = null
        uiState.collect { state ->
            val current = state.connection
            if (previous != null && previous !is ConnectionStatus.Disconnected && current is ConnectionStatus.Disconnected) {
                emit(Unit)
            }
            previous = current
        }
    }

    fun onInputChange(text: String) {
        state.update { it.copy(input = text, sendError = null) }
    }

    fun onSend() {
        val current = state.value
        if (!current.canSend) return
        val text = current.input
        state.update { it.copy(isSending = true, sendError = null) }
        viewModelScope.launch {
            sendMessage(text)
                .onSuccess { message ->
                    state.update {
                        it.copy(
                            messages = it.messages.plusCapped(message),
                            // Keep what the user typed meanwhile.
                            input = if (it.input == text) "" else it.input,
                            isSending = false,
                        )
                    }
                }
                .onFailure { error ->
                    val reason = (error as? ChannelException)?.error ?: ChannelError.Failed
                    state.update { it.copy(isSending = false, sendError = reason) }
                }
        }
    }

    fun onMenuOpen() {
        state.update { it.copy(isMenuOpen = true) }
    }

    /** Closed by tapping outside. */
    fun onMenuDismiss() {
        state.update { it.copy(isMenuOpen = false) }
    }

    /** Closes the menu first; the screen navigates for [action] once the menu is gone (see [onMenuActionHandled]). */
    fun onMenuItemClick(action: MenuAction) {
        state.update { it.copy(isMenuOpen = false, pendingMenuAction = action) }
    }

    fun onMenuActionHandled() {
        state.update { it.copy(pendingMenuAction = null) }
    }

    fun onDisconnect() {
        viewModelScope.launch { disconnect() }
    }

    private fun List<TerminalMessage>.plusCapped(message: TerminalMessage): List<TerminalMessage> =
        (this + message).takeLast(MAX_MESSAGES)

    companion object {
        /** Oldest messages are dropped beyond this, so a chatty device can't grow the log forever. */
        const val MAX_MESSAGES = 1_000
    }
}
