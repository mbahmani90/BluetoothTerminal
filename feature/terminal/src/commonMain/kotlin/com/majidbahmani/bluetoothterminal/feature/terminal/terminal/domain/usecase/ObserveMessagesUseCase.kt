package com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ChannelRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.model.TerminalMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

/**
 * Messages the device sends, one per notification, from every listened characteristic.
 *
 * Text is UTF-8; trailing line breaks are dropped (each message is already its own log entry) and a
 * notification that was only a line break is skipped. Notifications aren't joined into lines.
 */
internal class ObserveMessagesUseCase(private val repository: ChannelRepository) {

    operator fun invoke(): Flow<TerminalMessage> = repository.incoming.mapNotNull { data ->
        val text = data.bytes.decodeToString().trimEnd('\r', '\n')
        if (text.isEmpty()) null else TerminalMessage(TerminalMessage.Direction.Received, data.source, text)
    }
}
