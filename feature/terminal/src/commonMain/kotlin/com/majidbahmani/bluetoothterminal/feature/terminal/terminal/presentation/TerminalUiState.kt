package com.majidbahmani.bluetoothterminal.feature.terminal.terminal.presentation

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.model.TerminalMessage

/** Everything the Terminal screen shows (doc 29: no screen state in composables). */
internal data class TerminalUiState(
    val connection: ConnectionStatus = ConnectionStatus.Disconnected,
    /** Oldest first. */
    val messages: List<TerminalMessage> = emptyList(),
    val input: String = "",
    val isSending: Boolean = false,
    /** Why the last send failed; cleared when the input changes or the next send starts. */
    val sendError: ChannelError? = null,
) {
    val isConnected: Boolean get() = connection is ConnectionStatus.Connected

    val canSend: Boolean get() = isConnected && input.isNotEmpty() && !isSending
}
