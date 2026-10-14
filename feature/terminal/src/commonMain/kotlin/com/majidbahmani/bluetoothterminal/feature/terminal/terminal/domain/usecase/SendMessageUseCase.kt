package com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelException
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ChannelRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.model.TerminalMessage
import kotlinx.coroutines.flow.first

/**
 * Sends [text] followed by [LINE_ENDING] to the selected send target, so line-based devices see a whole line.
 *
 * Returns the sent message for the log, or a `ChannelException` (not connected, nothing to send to, …).
 */
internal class SendMessageUseCase(private val repository: ChannelRepository) {

    suspend operator fun invoke(text: String): Result<TerminalMessage> {
        val target = repository.selection.first().sendTarget
            ?: return Result.failure(ChannelException(ChannelError.NoSendTarget))
        return repository.send((text + LINE_ENDING).encodeToByteArray())
            .map { TerminalMessage(TerminalMessage.Direction.Sent, target, text) }
    }

    companion object {
        const val LINE_ENDING = "\n"
    }
}
