package com.majidbahmani.bluetoothterminal.feature.terminal.connection.data.repository

import com.majidbahmani.bluetooth.BluetoothSession
import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.BluetoothException
import com.majidbahmani.bluetooth.model.SessionState
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceInfo
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.repository.ConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** [ConnectionRepository] on top of the library's [BluetoothSession]; library types stop here. */
internal class ConnectionRepositoryImpl(private val session: BluetoothSession) : ConnectionRepository {

    override val status: Flow<ConnectionStatus> = session.state.map { it.toConnectionStatus() }

    override suspend fun disconnect() {
        session.disconnect()
    }
}

internal fun SessionState.toConnectionStatus(): ConnectionStatus = when (this) {
    SessionState.Idle -> ConnectionStatus.Disconnected
    is SessionState.Connecting -> ConnectionStatus.Connecting(device.toDeviceInfo())
    is SessionState.Connected -> ConnectionStatus.Connected(device.toDeviceInfo())
    is SessionState.Failed -> ConnectionStatus.Failed(device.toDeviceInfo(), cause.toConnectionError())
    is SessionState.Lost -> ConnectionStatus.Lost(device.toDeviceInfo())
}

internal fun BleDevice.toDeviceInfo(): DeviceInfo = DeviceInfo(id = id, name = name)

internal fun BluetoothException.toConnectionError(): ConnectionError = when (this) {
    is BluetoothException.NotAvailable -> ConnectionError.BluetoothUnavailable
    is BluetoothException.Timeout -> ConnectionError.Timeout
    else -> ConnectionError.Unknown
}
