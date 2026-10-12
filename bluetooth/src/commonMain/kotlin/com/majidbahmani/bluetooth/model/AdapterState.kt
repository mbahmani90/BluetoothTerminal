package com.majidbahmani.bluetooth.model

/** Whether BLE can be used. The library only reports it; the app asks for permissions. */
public enum class AdapterState {
    /** Not known yet (iOS, before CoreBluetooth reports its first state). */
    Unknown,

    /** No BLE hardware, e.g. emulator or simulator. */
    Unsupported,

    /** Bluetooth permission missing or denied. */
    Unauthorized,

    /** Bluetooth is switched off. */
    Off,

    /** Ready to scan and connect. */
    On,
}
