package com.majidbahmani.bluetoothterminal.feature.terminal.devices.fake

import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.BluetoothAvailability
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.Device
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/** Controllable [DeviceRepository]: set availability and devices in range; records scans and connect calls. */
internal class FakeDeviceRepository : DeviceRepository {

    override val availability = MutableStateFlow(BluetoothAvailability.Available)
    val devicesInRange = MutableStateFlow<List<Device>>(emptyList())

    /** When set, `scan()` fails with it. */
    var scanError: Throwable? = null

    var scansStarted = 0
        private set

    /** Scans running right now: 0 means the scan was stopped. */
    var activeScans = 0
        private set

    val connectRequests = mutableListOf<Device>()

    override fun scan(): Flow<List<Device>> = flow {
        scansStarted++
        activeScans++
        try {
            scanError?.let { throw it }
            devicesInRange.collect { emit(it) }
        } finally {
            activeScans--
        }
    }

    override suspend fun connect(device: Device) {
        connectRequests += device
    }
}
