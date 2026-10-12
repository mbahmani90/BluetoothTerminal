package com.majidbahmani.bluetooth.model

/** A GATT service: a group of characteristics, e.g. Nordic UART. UUIDs are canonical ([BluetoothUuid]). */
public data class GattService(
    val uuid: String,
    val characteristics: List<GattCharacteristic>,
)

/** One value on the device you can read, write or observe, depending on [properties]. */
public data class GattCharacteristic(
    val uuid: String,
    val properties: Set<CharacteristicProperty>,
)

public enum class CharacteristicProperty {
    Read,
    Write,
    WriteWithoutResponse,
    Notify,
    Indicate,
}

/** Address of a characteristic. Accepts any UUID form; equal ids compare equal whatever form was used. */
public class CharacteristicId(service: String, characteristic: String) {
    public val service: String = BluetoothUuid.normalize(service)
    public val characteristic: String = BluetoothUuid.normalize(characteristic)

    override fun equals(other: Any?): Boolean =
        other is CharacteristicId && service == other.service && characteristic == other.characteristic

    override fun hashCode(): Int = 31 * service.hashCode() + characteristic.hashCode()

    override fun toString(): String = "CharacteristicId(service=$service, characteristic=$characteristic)"
}

/** The characteristic [id] points to, or null if the device doesn't have it. */
public fun List<GattService>.find(id: CharacteristicId): GattCharacteristic? =
    firstOrNull { it.uuid == id.service }
        ?.characteristics
        ?.firstOrNull { it.uuid == id.characteristic }

/**
 * UUID handling shared by both platforms.
 *
 * BLE uses 16-bit (`FFE0`), 32-bit (`0000FFE0`) and 128-bit UUIDs. Short forms are shorthand for the
 * Bluetooth base UUID `0000xxxx-0000-1000-8000-00805f9b34fb`. The canonical form used everywhere in
 * this library is the 128-bit form in lowercase.
 */
public object BluetoothUuid {

    private const val BASE_SUFFIX = "-0000-1000-8000-00805f9b34fb"
    private val HEX = Regex("[0-9a-f]+")
    private val FULL = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")

    /** "FFE0", "0000ffe0" or the full form → "0000ffe0-0000-1000-8000-00805f9b34fb". */
    public fun normalize(uuid: String): String {
        val value = uuid.trim().lowercase()
        return when {
            value.length == 4 && HEX.matches(value) -> "0000$value$BASE_SUFFIX"
            value.length == 8 && HEX.matches(value) -> "$value$BASE_SUFFIX"
            FULL.matches(value) -> value
            else -> throw IllegalArgumentException("Not a Bluetooth UUID: '$uuid'")
        }
    }
}
