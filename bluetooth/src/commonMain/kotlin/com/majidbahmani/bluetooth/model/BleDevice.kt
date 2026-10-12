package com.majidbahmani.bluetooth.model

/**
 * A device found while scanning.
 *
 * [id] is opaque: the MAC address on Android, a random per-app UUID on iOS. Don't show it as a MAC.
 */
public data class BleDevice(
    val id: String,
    /** Advertised local name, or null if the device doesn't send one. */
    val name: String?,
    /** Signal strength in dBm (e.g. -60 is close, -95 is far). */
    val rssi: Int,
    /** Service UUIDs from the advertisement, in canonical form ([BluetoothUuid.normalize]). */
    val advertisedServices: List<String> = emptyList(),
)

/** Narrows a scan. Empty filter = every advertising device. */
public data class ScanFilter(
    /** Only devices advertising at least one of these services (any UUID form, see [BluetoothUuid]). */
    val serviceUuids: List<String> = emptyList(),
    /** Only devices whose name starts with this, ignoring case. */
    val namePrefix: String? = null,
)
