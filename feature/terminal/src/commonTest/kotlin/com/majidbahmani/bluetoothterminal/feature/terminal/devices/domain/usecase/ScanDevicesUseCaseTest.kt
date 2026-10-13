package com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.BluetoothAvailability
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.Device
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.DeviceScan
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.fake.FakeDeviceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class) // runCurrent()
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
