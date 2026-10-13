package com.majidbahmani.bluetoothterminal.feature.terminal.devices.presentation

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.BluetoothAvailability
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.DeviceScan

/** Everything the Devices screen shows (doc 29: no screen state in composables). */
internal data class DevicesUiState(
    val scan: DeviceScan = DeviceScan.Unavailable(BluetoothAvailability.Checking),
    val connection: ConnectionStatus = ConnectionStatus.Disconnected,
)
