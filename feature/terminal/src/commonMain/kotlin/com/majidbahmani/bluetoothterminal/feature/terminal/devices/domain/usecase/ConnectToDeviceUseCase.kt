package com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ConnectionRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.Device
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.repository.DeviceRepository
import kotlinx.coroutines.flow.first

/**
 * Connects to [Device]; the outcome (connected / failed) comes from the connection status.
 *
 * Rule: tapping the device that is already connecting or connected does nothing, so a double tap
 * doesn't drop a working connection. Another device replaces the current one.
 */
internal class ConnectToDeviceUseCase(
    private val devices: DeviceRepository,
    private val connection: ConnectionRepository,
) {

    suspend operator fun invoke(device: Device) {
        val activeId = when (val status = connection.status.first()) {
            is ConnectionStatus.Connecting -> status.device.id
            is ConnectionStatus.Connected -> status.device.id
            else -> null
        }
        if (activeId != device.id) devices.connect(device)
    }
}
