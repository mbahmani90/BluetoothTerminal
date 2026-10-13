package com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model

/** The device a connection is about. [name] is null when the device doesn't advertise one. */
internal data class DeviceInfo(
    val id: String,
    val name: String?,
)

/** Connection to the one active device, as both screens see it. */
internal sealed interface ConnectionStatus {

    /** No device: nothing connected yet, or the user disconnected. */
    data object Disconnected : ConnectionStatus

    data class Connecting(val device: DeviceInfo) : ConnectionStatus

    data class Connected(val device: DeviceInfo) : ConnectionStatus

    /** Connecting to [device] didn't work. */
    data class Failed(val device: DeviceInfo, val error: ConnectionError) : ConnectionStatus

    /** Was connected, then the link dropped (out of range, device switched off, …). */
    data class Lost(val device: DeviceInfo) : ConnectionStatus
}

/** Why connecting failed, in the terms the UI shows. The library's exceptions are mapped in `data`. */
internal enum class ConnectionError {
    /** Bluetooth is off, the permission is missing, or the phone has no BLE. */
    BluetoothUnavailable,

    /** The device didn't answer in time (out of range, switched off, already connected elsewhere). */
    Timeout,

    Unknown,
}
