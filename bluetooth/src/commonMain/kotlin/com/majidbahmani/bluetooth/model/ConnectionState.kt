package com.majidbahmani.bluetooth.model

/** State of a [com.majidbahmani.bluetooth.BleConnection]. A connection starts [Connected]. */
public sealed interface ConnectionState {

    public data object Connected : ConnectionState

    /** [cause] is null when the app disconnected; otherwise the link was lost (out of range, device off, …). */
    public data class Disconnected(val cause: BluetoothException? = null) : ConnectionState
}
