package com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.repository

import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.BluetoothAvailability
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.Device
import kotlinx.coroutines.flow.Flow

internal interface DeviceRepository {

    /** Current availability first, then every change (Bluetooth toggled, permission granted, …). */
    val availability: Flow<BluetoothAvailability>

    /**
     * Devices found so far. Scans while collected, stops when cancelled.
     * Fails if Bluetooth is unavailable or the platform stops the scan.
     */
    fun scan(): Flow<List<Device>>

    /** Starts connecting; the outcome is in the connection status (`ObserveConnectionUseCase`). */
    suspend fun connect(device: Device)
}
