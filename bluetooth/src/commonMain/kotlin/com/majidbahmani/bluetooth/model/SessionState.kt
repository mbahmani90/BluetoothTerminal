package com.majidbahmani.bluetooth.model

import com.majidbahmani.bluetooth.BleConnection

/** State of a [com.majidbahmani.bluetooth.BluetoothSession]: at most one device at a time. */
public sealed interface SessionState {

    /** No device: nothing connected yet, or the app disconnected. */
    public data object Idle : SessionState

    public data class Connecting(val device: BleDevice) : SessionState

    public data class Connected(val connection: BleConnection) : SessionState {
        val device: BleDevice get() = connection.device
    }

    /** Connecting failed or the link was lost; [cause] says why. */
    public data class Disconnected(val device: BleDevice, val cause: BluetoothException) : SessionState
}
