package com.majidbahmani.bluetoothterminal.feature.terminal.devices.data.repository

import com.majidbahmani.bluetooth.BluetoothSession
import com.majidbahmani.bluetooth.model.AdapterState
import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.SessionState
import com.majidbahmani.bluetooth.testing.FakeBluetoothClient
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.BluetoothAvailability
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.Device
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DeviceRepositoryImplTest {

    private val client = FakeBluetoothClient()

    @Test
    fun availability_mapsEveryAdapterState() = runTest {
        val repository = create()
        val expected = mapOf(
            AdapterState.Unknown to BluetoothAvailability.Checking,
            AdapterState.Unsupported to BluetoothAvailability.Unsupported,
            AdapterState.Unauthorized to BluetoothAvailability.PermissionMissing,
            AdapterState.Off to BluetoothAvailability.Off,
            AdapterState.On to BluetoothAvailability.Available,
        )

        expected.forEach { (adapterState, availability) ->
            client.adapterState.value = adapterState
            assertEquals(availability, repository.availability.first(), "for $adapterState")
        }
    }

    @Test
    fun scan_mapsBleDevicesToDevices() = runTest {
        client.devicesInRange.value = listOf(BleDevice(id = "1", name = "HM-10", rssi = -60))

        val devices = create().scan().first()

        assertEquals(listOf(Device(id = "1", name = "HM-10", signalStrength = -60)), devices)
    }

    @Test
    fun connect_connectsTheSessionToThatDevice() = runTest {
        val session = BluetoothSession(client, scope = backgroundScope)
        val repository = DeviceRepositoryImpl(client, session)

        repository.connect(Device(id = "1", name = "HM-10", signalStrength = -60))

        assertEquals(listOf("1"), client.connectRequests.map { it.id })
        assertIs<SessionState.Connected>(session.state.value)
    }

    private fun TestScope.create() = DeviceRepositoryImpl(client, BluetoothSession(client, scope = backgroundScope))
}
