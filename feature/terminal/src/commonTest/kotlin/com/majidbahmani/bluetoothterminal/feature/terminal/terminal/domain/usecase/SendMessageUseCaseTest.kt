package com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelException
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelSelection
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.fake.FakeChannelRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.model.TerminalMessage
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.model.TerminalMessage.Direction.Sent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SendMessageUseCaseTest {

    @Test
    fun send_addsLineEnding_andReturnsSentMessage() = runTest {
        val repository = FakeChannelRepository(ChannelSelection(sendTarget = RX))

        val result = SendMessageUseCase(repository)("hello")

        assertEquals(TerminalMessage(Sent, RX, "hello"), result.getOrThrow())
        assertEquals(listOf("hello\n"), repository.sent.map { it.decodeToString() })
    }

    @Test
    fun noSendTarget_failsWithoutSending() = runTest {
        val repository = FakeChannelRepository(ChannelSelection(sendTarget = null))

        val result = SendMessageUseCase(repository)("hello")

        assertEquals(ChannelError.NoSendTarget, (result.exceptionOrNull() as? ChannelException)?.error)
        assertEquals(emptyList(), repository.sent)
    }

    @Test
    fun sendFails_returnsTheError() = runTest {
        val repository = FakeChannelRepository(ChannelSelection(sendTarget = RX))
        repository.error = ChannelException(ChannelError.NotConnected)

        val result = SendMessageUseCase(repository)("hello")

        assertEquals(ChannelError.NotConnected, (result.exceptionOrNull() as? ChannelException)?.error)
    }

    private companion object {
        val RX = CharacteristicRef("6e400001-b5a3-f393-e0a9-e50e24dcca9e", "6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    }
}
