package com.majidbahmani.bluetooth.serial

import com.majidbahmani.bluetooth.model.BluetoothUuid
import com.majidbahmani.bluetooth.model.CharacteristicId
import com.majidbahmani.bluetooth.model.CharacteristicProperty
import com.majidbahmani.bluetooth.model.CharacteristicProperty.Indicate
import com.majidbahmani.bluetooth.model.CharacteristicProperty.Notify
import com.majidbahmani.bluetooth.model.CharacteristicProperty.Read
import com.majidbahmani.bluetooth.model.CharacteristicProperty.Write
import com.majidbahmani.bluetooth.model.CharacteristicProperty.WriteWithoutResponse
import com.majidbahmani.bluetooth.model.GattCharacteristic
import com.majidbahmani.bluetooth.model.GattService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SerialProfilesTest {

    @Test
    fun detect_nordicUart_isKnownProfile() {
        val services = listOf(GENERIC_ACCESS, nus())

        val channel = SerialProfiles.detect(services)

        assertEquals(
            SerialChannel(SerialProfiles.NordicUart.write, SerialProfiles.NordicUart.notify, SerialProfiles.NordicUart),
            channel,
        )
    }

    @Test
    fun detect_hm10_usesTheSameCharacteristicForBoth() {
        val services = listOf(service("ffe0", characteristic("ffe1", WriteWithoutResponse, Notify)))

        val channel = SerialProfiles.detect(services)

        assertEquals(SerialProfiles.Hm10, channel?.profile)
        assertEquals(CharacteristicId("ffe0", "ffe1"), channel?.write)
        assertEquals(CharacteristicId("ffe0", "ffe1"), channel?.notify)
    }

    @Test
    fun detect_knownUuidsWithWrongProperties_isNotTheKnownProfile() {
        // A clone with NUS UUIDs whose TX can't notify: not usable as Nordic UART.
        val services = listOf(nus(txProperties = arrayOf(Read)))

        assertEquals(null, SerialProfiles.detect(services)?.profile)
    }

    @Test
    fun detect_customService_picksWritableAndNotifyingCharacteristics() {
        val custom = service(
            CUSTOM_SERVICE,
            characteristic(CUSTOM_STATUS, Read),
            characteristic(CUSTOM_TX, Notify),
            characteristic(CUSTOM_RX, Write),
        )

        val channel = SerialProfiles.detect(listOf(GENERIC_ACCESS, custom))

        assertEquals(
            SerialChannel(CharacteristicId(CUSTOM_SERVICE, CUSTOM_RX), CharacteristicId(CUSTOM_SERVICE, CUSTOM_TX), profile = null),
            channel,
        )
    }

    @Test
    fun detect_indicateCountsAsNotify() {
        val custom = service(CUSTOM_SERVICE, characteristic(CUSTOM_RX, Write), characteristic(CUSTOM_TX, Indicate))

        assertEquals(CharacteristicId(CUSTOM_SERVICE, CUSTOM_TX), SerialProfiles.detect(listOf(custom))?.notify)
    }

    @Test
    fun detect_prefersCustomServiceOverSigService() {
        // Heart Rate (180D) has a notifying measurement and a writable control point, but listed first.
        val heartRate = service("180d", characteristic("2a37", Notify), characteristic("2a39", Write))
        val custom = service(CUSTOM_SERVICE, characteristic(CUSTOM_RX, Write), characteristic(CUSTOM_TX, Notify))

        assertEquals(CUSTOM_SERVICE, SerialProfiles.detect(listOf(heartRate, custom))?.write?.service)
    }

    @Test
    fun detect_writeAndNotifyInDifferentServices_isNull() {
        val services = listOf(
            service(CUSTOM_SERVICE, characteristic(CUSTOM_RX, Write)),
            service("180f", characteristic("2a19", Read, Notify)), // Battery
        )

        assertNull(SerialProfiles.detect(services))
    }

    @Test
    fun detect_noServices_isNull() {
        assertNull(SerialProfiles.detect(emptyList()))
    }

    private companion object {
        const val CUSTOM_SERVICE = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
        const val CUSTOM_RX = "beb5483e-36e1-4688-b7f5-ea07361b26a8"
        const val CUSTOM_TX = "beb5483f-36e1-4688-b7f5-ea07361b26a8"
        const val CUSTOM_STATUS = "beb54840-36e1-4688-b7f5-ea07361b26a8"

        // Device name is often writable, but nothing here notifies.
        val GENERIC_ACCESS = service("1800", characteristic("2a00", Read, Write), characteristic("2a01", Read))

        fun nus(txProperties: Array<CharacteristicProperty> = arrayOf(Notify)) = service(
            "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
            characteristic("6e400002-b5a3-f393-e0a9-e50e24dcca9e", Write, WriteWithoutResponse),
            characteristic("6e400003-b5a3-f393-e0a9-e50e24dcca9e", *txProperties),
        )

        fun service(uuid: String, vararg characteristics: GattCharacteristic) =
            GattService(BluetoothUuid.normalize(uuid), characteristics.toList())

        fun characteristic(uuid: String, vararg properties: CharacteristicProperty) =
            GattCharacteristic(BluetoothUuid.normalize(uuid), properties.toSet())
    }
}
