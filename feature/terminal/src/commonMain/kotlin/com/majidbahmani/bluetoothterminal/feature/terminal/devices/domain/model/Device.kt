package com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model

/** A device found nearby. [name] is null when it doesn't advertise one; [signalStrength] is in dBm (-60 close, -95 far). */
internal data class Device(
    val id: String,
    val name: String?,
    val signalStrength: Int,
)

/** Whether the phone can scan right now; anything but [Available] says what the user has to fix. */
internal enum class BluetoothAvailability {
    /** Not known yet (iOS, right after start). */
    Checking,

    /** The phone has no Bluetooth Low Energy (or it's an emulator/simulator). */
    Unsupported,

    PermissionMissing,

    Off,

    Available,
}

/** What the Devices screen shows about scanning. */
internal sealed interface DeviceScan {

    /** No scan: Bluetooth can't be used; [availability] says why. */
    data class Unavailable(val availability: BluetoothAvailability) : DeviceScan

    /** Scanning; [devices] found so far, in discovery order. */
    data class Scanning(val devices: List<Device>) : DeviceScan

    /** The scan ran its full time or the user stopped it; [devices] is what it found. */
    data class Finished(val devices: List<Device>) : DeviceScan

    /** The platform stopped the scan with an error. */
    data object Failed : DeviceScan
}
