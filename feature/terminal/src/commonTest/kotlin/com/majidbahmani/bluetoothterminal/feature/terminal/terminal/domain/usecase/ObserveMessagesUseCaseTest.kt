package com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.IncomingData
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.fake.FakeChannelRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.model.TerminalMessage
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.model.TerminalMessage.Direction.Received
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class) // runCurrent()
class ObserveMessagesUseCaseTest {

    private val repository = FakeChannelRepository()

    @Test
    fun incoming_becomesReceivedMessages_withTheirCharacteristic() = runTest {
        val messages = collect()

        receive(TX, "tick 1\r\n")
        receive(OTHER, "42")
        runCurrent()

        assertEquals(listOf(TerminalMessage(Received, TX, "tick 1"), TerminalMessage(Received, OTHER, "42")), messages)
    }

    @Test
    fun lineBreakOnly_isSkipped() = runTest {
        val messages = collect()

        receive(TX, "\r\n")
        runCurrent()

        assertEquals(emptyList(), messages)
    }

    @Test
    fun leadingAndInnerSpaces_areKept() = runTest {
        val messages = collect()

        receive(TX, "  a  b\n")
        runCurrent()

        assertEquals("  a  b", messages.single().text)
    }

    private fun TestScope.collect(): List<TerminalMessage> {
        val messages = mutableListOf<TerminalMessage>()
        backgroundScope.launch { ObserveMessagesUseCase(repository)().collect { messages += it } }
        runCurrent()
        return messages
    }

    private fun receive(source: CharacteristicRef, text: String) {
        repository.incoming.tryEmit(IncomingData(source, text.encodeToByteArray()))
    }

    private companion object {
        val TX = CharacteristicRef("6e400001-b5a3-f393-e0a9-e50e24dcca9e", "6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val OTHER = CharacteristicRef("4fafc201-1fb5-459e-8fcc-c5c9c331914b", "beb5483f-36e1-4688-b7f5-ea07361b26a8")
    }
}
