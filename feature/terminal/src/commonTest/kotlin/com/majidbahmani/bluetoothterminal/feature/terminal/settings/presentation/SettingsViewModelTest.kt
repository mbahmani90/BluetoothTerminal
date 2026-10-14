package com.majidbahmani.bluetoothterminal.feature.terminal.settings.presentation

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelException
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelSelection
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceCharacteristic
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceService
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.fake.FakeChannelRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.ObserveServiceSettingsUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.ReadCharacteristicUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.SetListeningUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.usecase.SetSendTargetUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class) // setMain, runCurrent
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeChannelRepository(ChannelSelection(sendTarget = RX.ref, listening = setOf(TX.ref))).apply {
        services.value = listOf(DeviceService(NUS, listOf(RX, TX, NAME)))
    }

    // viewModelScope uses Dispatchers.Main; replace it with the test scheduler (doc 22).
    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun uiState_listsServicesWithSelection() = runTest(dispatcher) {
        val viewModel = createAndCollect()

        val characteristics = viewModel.uiState.value.services.single().characteristics
        assertEquals(listOf(true, false, false), characteristics.map { it.isSendTarget })
        assertEquals(listOf(false, true, false), characteristics.map { it.isListening })
    }

    @Test
    fun notConnected_hasNoServices() = runTest(dispatcher) {
        repository.services.value = emptyList()
        val viewModel = createAndCollect()

        assertTrue(viewModel.uiState.value.services.isEmpty())
    }

    @Test
    fun onSendTargetSelect_andOnListeningChange_updateTheList() = runTest(dispatcher) {
        val viewModel = createAndCollect()

        viewModel.onSendTargetSelect(NAME.ref)
        viewModel.onListeningChange(TX.ref, false)
        runCurrent()

        val characteristics = viewModel.uiState.value.services.single().characteristics
        assertEquals(listOf(false, false, true), characteristics.map { it.isSendTarget })
        assertEquals(listOf(false, false, false), characteristics.map { it.isListening })
    }

    @Test
    fun onRead_text_showsText() = runTest(dispatcher) {
        repository.readValues[NAME.ref] = "ESP32-NUS".encodeToByteArray()
        val viewModel = createAndCollect()

        viewModel.onRead(NAME.ref)
        runCurrent()

        assertEquals(ReadResult.Value("ESP32-NUS"), viewModel.uiState.value.reads[NAME.ref])
    }

    @Test
    fun onRead_binary_showsHex() = runTest(dispatcher) {
        repository.readValues[NAME.ref] = byteArrayOf(0x00, 0x0A, 0xFF.toByte())
        val viewModel = createAndCollect()

        viewModel.onRead(NAME.ref)
        runCurrent()

        assertEquals(ReadResult.Value("00 0A FF"), viewModel.uiState.value.reads[NAME.ref])
    }

    @Test
    fun onRead_fails_showsError() = runTest(dispatcher) {
        repository.error = ChannelException(ChannelError.NotConnected)
        val viewModel = createAndCollect()

        viewModel.onRead(NAME.ref)
        runCurrent()

        assertEquals(ReadResult.Failed(ChannelError.NotConnected), viewModel.uiState.value.reads[NAME.ref])
    }

    private fun TestScope.createAndCollect(): SettingsViewModel {
        val viewModel = SettingsViewModel(
            observeServiceSettings = ObserveServiceSettingsUseCase(repository),
            setSendTarget = SetSendTargetUseCase(repository),
            setListening = SetListeningUseCase(repository),
            readCharacteristic = ReadCharacteristicUseCase(repository),
        )
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    private companion object {
        const val NUS = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
        val RX = DeviceCharacteristic(CharacteristicRef(NUS, "6e400002-b5a3-f393-e0a9-e50e24dcca9e"), canRead = false, canWrite = true, canNotify = false)
        val TX = DeviceCharacteristic(CharacteristicRef(NUS, "6e400003-b5a3-f393-e0a9-e50e24dcca9e"), canRead = false, canWrite = false, canNotify = true)
        val NAME = DeviceCharacteristic(CharacteristicRef(NUS, "6e400004-b5a3-f393-e0a9-e50e24dcca9e"), canRead = true, canWrite = true, canNotify = false)
    }
}
