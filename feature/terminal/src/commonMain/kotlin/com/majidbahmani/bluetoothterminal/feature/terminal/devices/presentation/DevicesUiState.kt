package com.majidbahmani.bluetoothterminal.feature.terminal.devices.presentation

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.BluetoothAvailability
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.DeviceScan

/** Everything the Devices screen shows (doc 29: no screen state in composables). */
internal data class DevicesUiState(
    val scan: DeviceScan = DeviceScan.Unavailable(BluetoothAvailability.Checking),
    val connection: ConnectionStatus = ConnectionStatus.Disconnected,
) {
    /** True while [connection] pauses the scan; [scan] then holds the last list found. */
    val isScanPaused: Boolean get() = connection.pausesScan
}

/**
 * No scanning while connecting or connected. On Android, a scan during a connect makes connection errors
 * (status 133) more likely, and a scan during a connection costs battery and radio time for the link.
 */
internal val ConnectionStatus.pausesScan: Boolean
    get() = this is ConnectionStatus.Connecting || this is ConnectionStatus.Connected
