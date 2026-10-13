package com.majidbahmani.bluetoothterminal.feature.terminal.devices.presentation

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceInfo
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.usecase.ObserveConnectionUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.fake.FakeConnectionRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.BluetoothAvailability
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.Device
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.DeviceScan
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase.ConnectToDeviceUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase.ScanDevicesUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.fake.FakeDeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class) // setMain, runCurrent, advanceTimeBy
class DevicesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val devices = FakeDeviceRepository()
    private val connection = FakeConnectionRepository()

    // viewModelScope uses Dispatchers.Main; replace it with the test scheduler (doc 22).
    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun uiState_showsDevicesFound() = runTest(dispatcher) {
        devices.devicesInRange.value = listOf(HM10)
        val viewModel = createAndCollect()

        assertEquals(DeviceScan.Scanning(listOf(HM10)), viewModel.uiState.value.scan)
    }

    @Test
    fun uiState_bluetoothOff_isUnavailable() = runTest(dispatcher) {
        devices.availability.value = BluetoothAvailability.Off
        val viewModel = createAndCollect()

        assertEquals(DeviceScan.Unavailable(BluetoothAvailability.Off), viewModel.uiState.value.scan)
    }

    @Test
    fun uiState_followsConnectionStatus() = runTest(dispatcher) {
        val viewModel = createAndCollect()

        connection.status.value = ConnectionStatus.Failed(HM10_INFO, ConnectionError.Timeout)
        runCurrent()

        assertEquals(ConnectionStatus.Failed(HM10_INFO, ConnectionError.Timeout), viewModel.uiState.value.connection)
    }

    @Test
    fun onDeviceClick_connects() = runTest(dispatcher) {
        val viewModel = createAndCollect()

        viewModel.onDeviceClick(HM10)
        runCurrent()

        assertEquals(listOf(HM10), devices.connectRequests)
    }

    @Test
    fun connecting_pausesScanAndKeepsList() = runTest(dispatcher) {
        devices.devicesInRange.value = listOf(HM10)
        val viewModel = createAndCollect()

        connection.status.value = ConnectionStatus.Connecting(HM10_INFO)
        runCurrent()

        assertEquals(0, devices.activeScans)
        assertEquals(DeviceScan.Scanning(listOf(HM10)), viewModel.uiState.value.scan)
    }

    @Test
    fun connected_keepsScanPausedAndList() = runTest(dispatcher) {
        devices.devicesInRange.value = listOf(HM10)
        val viewModel = createAndCollect()
        connection.status.value = ConnectionStatus.Connecting(HM10_INFO)
        runCurrent()

        connection.status.value = ConnectionStatus.Connected(HM10_INFO)
        runCurrent()

        assertEquals(0, devices.activeScans)
        assertEquals(1, devices.scansStarted)
        assertEquals(DeviceScan.Scanning(listOf(HM10)), viewModel.uiState.value.scan)
        assertTrue(viewModel.uiState.value.isScanPaused)
    }

    @Test
    fun connectionLost_resumesScan() = runTest(dispatcher) {
        val viewModel = createAndCollect()
        connection.status.value = ConnectionStatus.Connected(HM10_INFO)
        runCurrent()

        connection.status.value = ConnectionStatus.Lost(HM10_INFO)
        runCurrent()

        assertEquals(1, devices.activeScans)
        assertEquals(2, devices.scansStarted)
        assertFalse(viewModel.uiState.value.isScanPaused)
    }

    @Test
    fun connectFailed_resumesScan() = runTest(dispatcher) {
        createAndCollect()
        connection.status.value = ConnectionStatus.Connecting(HM10_INFO)
        runCurrent()

        connection.status.value = ConnectionStatus.Failed(HM10_INFO, ConnectionError.Timeout)
        runCurrent()

        assertEquals(1, devices.activeScans)
        assertEquals(2, devices.scansStarted)
    }

    @Test
    fun onRetry_afterFailure_scansAgain() = runTest(dispatcher) {
        devices.scanError = IllegalStateException("scan failed")
        val viewModel = createAndCollect()
        assertEquals(DeviceScan.Failed, viewModel.uiState.value.scan)

        devices.scanError = null
        devices.devicesInRange.value = listOf(HM10)
        viewModel.onRetry()
        runCurrent()

        assertEquals(DeviceScan.Scanning(listOf(HM10)), viewModel.uiState.value.scan)
    }

    @Test
    fun screenGone_stopsScanAfterTimeout() = runTest(dispatcher) {
        val viewModel = create()
        val collector = backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        assertEquals(1, devices.activeScans)

        collector.cancel()
        advanceTimeBy(5_001)

        assertEquals(0, devices.activeScans)
    }

    private fun create() = DevicesViewModel(
        scanDevices = ScanDevicesUseCase(devices),
        connectToDevice = ConnectToDeviceUseCase(devices, connection),
        observeConnection = ObserveConnectionUseCase(connection),
    )

    /** uiState is WhileSubscribed: it only scans while someone collects it, like the screen does. */
    private fun TestScope.createAndCollect(): DevicesViewModel {
        val viewModel = create()
        backgroundScope.launch { viewModel.uiState.collect {} }
        runCurrent()
        return viewModel
    }

    private companion object {
        val HM10 = Device(id = "1", name = "HM-10", signalStrength = -60)
        val HM10_INFO = DeviceInfo(id = "1", name = "HM-10")
    }
}
