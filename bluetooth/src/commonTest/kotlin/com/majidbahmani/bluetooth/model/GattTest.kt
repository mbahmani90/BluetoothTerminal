package com.majidbahmani.bluetooth.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

class GattTest {

    @Test
    fun normalize_16BitUuid_expandsToBaseUuid() {
        assertEquals("0000ffe0-0000-1000-8000-00805f9b34fb", BluetoothUuid.normalize("FFE0"))
    }

    @Test
    fun normalize_32BitUuid_expandsToBaseUuid() {
        assertEquals("0000ffe0-0000-1000-8000-00805f9b34fb", BluetoothUuid.normalize("0000FFE0"))
    }

    @Test
    fun normalize_128BitUuid_isLowercasedAndTrimmed() {
        assertEquals(
            "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
            BluetoothUuid.normalize(" 6E400001-B5A3-F393-E0A9-E50E24DCCA9E "),
        )
    }

    @Test
    fun normalize_invalidUuid_throws() {
        listOf("", "FFE", "XYZ0", "0000ffe0-0000-1000-8000", "6e400001b5a3f393e0a9e50e24dcca9e").forEach {
            assertFailsWith<IllegalArgumentException>(it) { BluetoothUuid.normalize(it) }
        }
    }

    @Test
    fun characteristicId_differentUuidForms_areEqual() {
        val short = CharacteristicId(service = "FFE0", characteristic = "FFE1")
        val full = CharacteristicId(
            service = "0000ffe0-0000-1000-8000-00805f9b34fb",
            characteristic = "0000FFE1-0000-1000-8000-00805F9B34FB",
        )

        assertEquals(short, full)
        assertEquals(short.hashCode(), full.hashCode())
    }

    @Test
    fun find_existingCharacteristic_returnsIt() {
        assertSame(hm10Characteristic, services.find(CharacteristicId("FFE0", "FFE1")))
    }

    @Test
    fun find_missingServiceOrCharacteristic_returnsNull() {
        assertNull(services.find(CharacteristicId("FFF0", "FFE1")))
        assertNull(services.find(CharacteristicId("FFE0", "FFF1")))
    }

    private val hm10Characteristic = GattCharacteristic(
        uuid = BluetoothUuid.normalize("FFE1"),
        properties = setOf(CharacteristicProperty.WriteWithoutResponse, CharacteristicProperty.Notify),
    )

    private val services = listOf(
        GattService(uuid = BluetoothUuid.normalize("180A"), characteristics = emptyList()),
        GattService(uuid = BluetoothUuid.normalize("FFE0"), characteristics = listOf(hm10Characteristic)),
    )
}
