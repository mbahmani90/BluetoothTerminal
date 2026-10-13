package com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.BluetoothAvailability
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.Device
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.DeviceScan
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.fake.FakeDeviceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class) // runCurrent(), advanceTimeBy()
class ScanDevicesUseCaseTest {

    private val repository = FakeDeviceRepository()
    private val scanDevices = ScanDevicesUseCase(repository)

    @Test
    fun bluetoothOff_isUnavailableWithoutScanning() = runTest {
        repository.availability.value = BluetoothAvailability.Off

        assertEquals(DeviceScan.Unavailable(BluetoothAvailability.Off), scanDevices().first())
        assertEquals(0, repository.scansStarted)
    }

    @Test
    fun available_scansAndHidesUnnamedDevices() = runTest {
        repository.devicesInRange.value = listOf(HM10, UNNAMED)

        assertEquals(DeviceScan.Scanning(listOf(HM10)), scanDevices().first())
    }

    @Test
    fun showUnnamed_keepsUnnamedDevices() = runTest {
        repository.devicesInRange.value = listOf(HM10, UNNAMED)

        assertEquals(DeviceScan.Scanning(listOf(HM10, UNNAMED)), scanDevices(showUnnamed = true).first())
    }

    @Test
    fun becomesAvailable_startsScanning() = runTest {
        repository.availability.value = BluetoothAvailability.PermissionMissing
        val scans = collect()

        repository.availability.value = BluetoothAvailability.Available
        runCurrent()

        assertEquals(
            listOf(DeviceScan.Unavailable(BluetoothAvailability.PermissionMissing), DeviceScan.Scanning(emptyList())),
            scans,
        )
        assertEquals(1, repository.scansStarted)
    }

    @Test
    fun becomesUnavailable_stopsScanning() = runTest {
        val scans = collect()

        repository.availability.value = BluetoothAvailability.Off
        runCurrent()

        assertEquals(DeviceScan.Unavailable(BluetoothAvailability.Off), scans.last())
        assertEquals(0, repository.activeScans)
    }

    @Test
    fun scanDurationPassed_stopsAndKeepsDevices() = runTest {
        repository.devicesInRange.value = listOf(HM10, UNNAMED)
        val scans = collect()

        advanceTimeBy(ScanDevicesUseCase.SCAN_DURATION)
        runCurrent()

        assertEquals(DeviceScan.Finished(listOf(HM10)), scans.last())
        assertEquals(0, repository.activeScans)
    }

    @Test
    fun beforeScanDuration_keepsScanning() = runTest {
        val scans = collect()

        advanceTimeBy(ScanDevicesUseCase.SCAN_DURATION - 1.seconds)

        assertEquals(DeviceScan.Scanning(emptyList()), scans.last())
        assertEquals(1, repository.activeScans)
    }

    @Test
    fun stopRequested_stopsAndKeepsDevices() = runTest {
        repository.devicesInRange.value = listOf(HM10, UNNAMED)
        val stop = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val scans = mutableListOf<DeviceScan>()
        backgroundScope.launch { scanDevices(stop = stop).collect { scans += it } }
        runCurrent()

        stop.tryEmit(Unit)
        runCurrent()

        assertEquals(listOf(DeviceScan.Scanning(listOf(HM10)), DeviceScan.Finished(listOf(HM10))), scans)
        assertEquals(0, repository.activeScans)
    }

    @Test
    fun scanFails_isFailed() = runTest {
        repository.scanError = IllegalStateException("scan failed")

        assertEquals(DeviceScan.Failed, scanDevices().first())
    }

    private fun TestScope.collect(): List<DeviceScan> {
        val scans = mutableListOf<DeviceScan>()
        backgroundScope.launch { scanDevices().collect { scans += it } }
        runCurrent()
        return scans
    }

    private companion object {
        val HM10 = Device(id = "1", name = "HM-10", signalStrength = -60)
        val UNNAMED = Device(id = "2", name = null, signalStrength = -80)
    }
}
