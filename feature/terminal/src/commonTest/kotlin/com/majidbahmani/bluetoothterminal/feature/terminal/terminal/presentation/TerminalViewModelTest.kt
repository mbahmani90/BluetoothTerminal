package com.majidbahmani.bluetoothterminal.feature.terminal.terminal.presentation

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelException
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelSelection
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceInfo
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.IncomingData
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase.DisconnectUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase.ObserveConnectionUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.fake.FakeChannelRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.fake.FakeConnectionRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.model.TerminalMessage
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.model.TerminalMessage.Direction.Received
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.model.TerminalMessage.Direction.Sent
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase.ObserveMessagesUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class) // setMain, runCurrent
class TerminalViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val connection = FakeConnectionRepository(ConnectionStatus.Connected(ESP32))
    private val channel = FakeChannelRepository(ChannelSelection(sendTarget = RX, listening = setOf(TX)))

    // viewModelScope uses Dispatchers.Main; replace it with the test scheduler (doc 22).
    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun uiState_followsConnection() = runTest(dispatcher) {
        val viewModel = create()

        connection.status.value = ConnectionStatus.Lost(ESP32)
        runCurrent()

        assertEquals(ConnectionStatus.Lost(ESP32), viewModel.uiState.value.connection)
        assertFalse(viewModel.uiState.value.isConnected)
    }

    @Test
    fun receivedMessages_areAddedToTheLog() = runTest(dispatcher) {
        val viewModel = create()

        receive("tick 1\r\n")
        receive("tick 2\r\n")
        runCurrent()

        assertEquals(
            listOf(TerminalMessage(Received, TX, "tick 1"), TerminalMessage(Received, TX, "tick 2")),
            viewModel.uiState.value.messages,
        )
    }

    @Test
    fun onSend_sendsAddsToLogAndClearsInput() = runTest(dispatcher) {
        val viewModel = create()
        viewModel.onInputChange("hello")

        viewModel.onSend()
        runCurrent()

        assertEquals(listOf("hello\n"), channel.sent.map { it.decodeToString() })
        assertEquals(listOf(TerminalMessage(Sent, RX, "hello")), viewModel.uiState.value.messages)
        assertEquals("", viewModel.uiState.value.input)
        assertFalse(viewModel.uiState.value.isSending)
    }

    @Test
    fun onSend_emptyInput_sendsNothing() = runTest(dispatcher) {
        val viewModel = create()

        viewModel.onSend()
        runCurrent()

        assertEquals(emptyList(), channel.sent)
    }

    @Test
    fun onSend_notConnected_sendsNothing() = runTest(dispatcher) {
        connection.status.value = ConnectionStatus.Lost(ESP32)
        val viewModel = create()
        viewModel.onInputChange("hello")

        viewModel.onSend()
        runCurrent()

        assertEquals(emptyList(), channel.sent)
    }

    @Test
    fun onSend_noSendTarget_showsErrorAndKeepsInput() = runTest(dispatcher) {
        channel.selection.value = ChannelSelection(sendTarget = null)
        val viewModel = create()
        viewModel.onInputChange("hello")

        viewModel.onSend()
        runCurrent()

        assertEquals(ChannelError.NoSendTarget, viewModel.uiState.value.sendError)
        assertEquals("hello", viewModel.uiState.value.input)
        assertEquals(emptyList(), viewModel.uiState.value.messages)
    }

    @Test
    fun onSend_failed_showsError_untilInputChanges() = runTest(dispatcher) {
        channel.error = ChannelException(ChannelError.Failed)
        val viewModel = create()
        viewModel.onInputChange("hello")
        viewModel.onSend()
        runCurrent()
        assertEquals(ChannelError.Failed, viewModel.uiState.value.sendError)

        viewModel.onInputChange("hello!")

        assertNull(viewModel.uiState.value.sendError)
    }

    @Test
    fun canSend_onlyWhenConnectedWithInputAndNotSending() = runTest(dispatcher) {
        val viewModel = create()
        assertFalse(viewModel.uiState.value.canSend)

        viewModel.onInputChange("hello")
        assertTrue(viewModel.uiState.value.canSend)

        viewModel.onSend() // sending until the coroutine runs
        assertFalse(viewModel.uiState.value.canSend)
    }

    @Test
    fun log_keepsOnlyTheNewestMessages() = runTest(dispatcher) {
        val viewModel = create()

        repeat(TerminalViewModel.MAX_MESSAGES + 5) {
            receive("m$it")
            runCurrent() // the fake's buffer is small: let the view model take each one
        }

        val messages = viewModel.uiState.value.messages
        assertEquals(TerminalViewModel.MAX_MESSAGES, messages.size)
        assertEquals("m5", messages.first().text)
    }

    @Test
    fun onDisconnect_disconnects() = runTest(dispatcher) {
        val viewModel = create()

        viewModel.onDisconnect()
        runCurrent()

        assertEquals(1, connection.disconnectCalls)
        assertEquals(ConnectionStatus.Disconnected, viewModel.uiState.value.connection)
    }

    private fun create(): TerminalViewModel = TerminalViewModel(
        observeConnection = ObserveConnectionUseCase(connection),
        observeMessages = ObserveMessagesUseCase(channel),
        sendMessage = SendMessageUseCase(channel),
        disconnect = DisconnectUseCase(connection),
    ).also { dispatcher.scheduler.runCurrent() }

    private fun receive(text: String) {
        channel.incoming.tryEmit(IncomingData(TX, text.encodeToByteArray()))
    }

    private companion object {
        val ESP32 = DeviceInfo(id = "1", name = "ESP32-NUS")
        val RX = CharacteristicRef("6e400001-b5a3-f393-e0a9-e50e24dcca9e", "6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        val TX = CharacteristicRef("6e400001-b5a3-f393-e0a9-e50e24dcca9e", "6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    }
}
