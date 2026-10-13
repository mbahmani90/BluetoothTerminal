package com.majidbahmani.bluetoothterminal.feature.terminal.devices.data.repository

import com.majidbahmani.bluetooth.BluetoothClient
import com.majidbahmani.bluetooth.BluetoothSession
import com.majidbahmani.bluetooth.model.AdapterState
import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.BluetoothAvailability
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.Device
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [DeviceRepository] on top of the library: scanning via [BluetoothClient], connecting via the shared [BluetoothSession]. */
internal class DeviceRepositoryImpl(
    private val client: BluetoothClient,
    private val session: BluetoothSession,
) : DeviceRepository {

    override val availability: Flow<BluetoothAvailability> = client.adapterState.map { it.toAvailability() }

    override fun scan(): Flow<List<Device>> = client.scan().map { devices -> devices.map { it.toDevice() } }

    override suspend fun connect(device: Device) {
        // The result is reported through the session's state, which the connection package maps.
        session.connect(device.toBleDevice())
    }
}

internal fun AdapterState.toAvailability(): BluetoothAvailability = when (this) {
    AdapterState.Unknown -> BluetoothAvailability.Checking
    AdapterState.Unsupported -> BluetoothAvailability.Unsupported
    AdapterState.Unauthorized -> BluetoothAvailability.PermissionMissing
    AdapterState.Off -> BluetoothAvailability.Off
    AdapterState.On -> BluetoothAvailability.Available
}

internal fun BleDevice.toDevice(): Device = Device(id = id, name = name, signalStrength = rssi)

/** Connecting only needs the id; name and RSSI are kept for the session's state. */
internal fun Device.toBleDevice(): BleDevice = BleDevice(id = id, name = name, rssi = signalStrength)
