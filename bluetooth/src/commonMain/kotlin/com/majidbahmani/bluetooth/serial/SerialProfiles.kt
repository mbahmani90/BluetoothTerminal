package com.majidbahmani.bluetooth.serial

import com.majidbahmani.bluetooth.model.CharacteristicId
import com.majidbahmani.bluetooth.model.CharacteristicProperty
import com.majidbahmani.bluetooth.model.GattCharacteristic
import com.majidbahmani.bluetooth.model.GattService
import com.majidbahmani.bluetooth.model.find

/**
 * A well-known "serial port over BLE" layout: the phone writes to [write] and listens to [notify].
 * Both can be the same characteristic (HM-10).
 */
public data class SerialProfile(
    val name: String,
    val write: CharacteristicId,
    val notify: CharacteristicId,
)

/**
 * The characteristics to use as a serial port on one device.
 *
 * [profile] is the known profile they came from, or null when they were picked by their properties.
 */
public data class SerialChannel(
    val write: CharacteristicId,
    val notify: CharacteristicId,
    val profile: SerialProfile?,
)

/** Known serial profiles, and [detect] to pick a default channel on any device. */
public object SerialProfiles {

    private const val NUS = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
    private const val MICROCHIP = "49535343-fe7d-4ae5-8fa9-9fafd205e455"

    /** Suffix of UUIDs assigned by the Bluetooth SIG (and of 16-bit vendor ones like FFE0). */
    private const val BASE_SUFFIX = "-0000-1000-8000-00805f9b34fb"

    public val NordicUart: SerialProfile = SerialProfile(
        name = "Nordic UART",
        write = CharacteristicId(NUS, "6e400002-b5a3-f393-e0a9-e50e24dcca9e"),
        notify = CharacteristicId(NUS, "6e400003-b5a3-f393-e0a9-e50e24dcca9e"),
    )

    public val Hm10: SerialProfile = SerialProfile(
        name = "HM-10",
        write = CharacteristicId("ffe0", "ffe1"),
        notify = CharacteristicId("ffe0", "ffe1"),
    )

    /** Used by many BLE OBD-II dongles. */
    public val Fff0: SerialProfile = SerialProfile(
        name = "FFF0",
        write = CharacteristicId("fff0", "fff2"),
        notify = CharacteristicId("fff0", "fff1"),
    )

    public val MicrochipTransparentUart: SerialProfile = SerialProfile(
        name = "Microchip Transparent UART",
        write = CharacteristicId(MICROCHIP, "49535343-8841-43f4-a8d4-ecbe34729bb3"),
        notify = CharacteristicId(MICROCHIP, "49535343-1e4d-4bd9-ba61-23c647249616"),
    )

    /** In the order [detect] tries them. */
    public val known: List<SerialProfile> = listOf(NordicUart, Hm10, Fff0, MicrochipTransparentUart)

    /**
     * The default channel for a device with [services], or null if it has nothing to write to and listen on.
     *
     * 1. The first [known] profile whose characteristics exist with the right properties
     *    (a UUID match alone isn't enough: clones vary).
     * 2. Otherwise the first service with a writable and a notifying characteristic. Custom (128-bit)
     *    services are tried before SIG-assigned ones, so e.g. Heart Rate isn't picked over a vendor service.
     */
    public fun detect(services: List<GattService>): SerialChannel? {
        known.firstOrNull { profile ->
            services.find(profile.write)?.canWrite() == true && services.find(profile.notify)?.canNotify() == true
        }?.let { return SerialChannel(it.write, it.notify, profile = it) }

        return services
            .sortedBy { it.uuid.endsWith(BASE_SUFFIX) } // stable: custom first, original order kept
            .firstNotNullOfOrNull { service ->
                val write = service.characteristics.firstOrNull { it.canWrite() } ?: return@firstNotNullOfOrNull null
                val notify = service.characteristics.firstOrNull { it.canNotify() } ?: return@firstNotNullOfOrNull null
                SerialChannel(
                    write = CharacteristicId(service.uuid, write.uuid),
                    notify = CharacteristicId(service.uuid, notify.uuid),
                    profile = null,
                )
            }
    }
}

/** Write with or without response. */
public fun GattCharacteristic.canWrite(): Boolean =
    CharacteristicProperty.Write in properties || CharacteristicProperty.WriteWithoutResponse in properties

/** Notify or indicate: the device sends values on its own. */
public fun GattCharacteristic.canNotify(): Boolean =
    CharacteristicProperty.Notify in properties || CharacteristicProperty.Indicate in properties
