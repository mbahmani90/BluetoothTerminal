package com.majidbahmani.bluetooth.serial

import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.BluetoothException
import com.majidbahmani.bluetooth.model.BluetoothUuid
import com.majidbahmani.bluetooth.model.CharacteristicId
import com.majidbahmani.bluetooth.model.CharacteristicProperty
import com.majidbahmani.bluetooth.model.CharacteristicProperty.Notify
import com.majidbahmani.bluetooth.model.CharacteristicProperty.Write
import com.majidbahmani.bluetooth.model.CharacteristicProperty.WriteWithoutResponse
import com.majidbahmani.bluetooth.model.GattCharacteristic
import com.majidbahmani.bluetooth.model.GattService
import com.majidbahmani.bluetooth.testing.FakeBleConnection
import com.majidbahmani.bluetooth.testing.FakeBleConnection.Write as WriteCall
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChunkedWriteTest {

    @Test
    fun shortMessage_isOneWrite() = runTest {
        val connection = connection(WriteWithoutResponse)

        val result = connection.writeChunked(RX, "hello".encodeToByteArray())

        assertTrue(result.isSuccess)
        assertEquals(listOf(WriteCall(RX, "hello".encodeToByteArray(), withResponse = false)), connection.writes)
    }

    @Test
    fun longMessage_isSplitIntoMaxWriteLengthChunksInOrder() = runTest {
        val connection = connection(WriteWithoutResponse, maxWriteLength = 4)

        connection.writeChunked(RX, "abcdefghij".encodeToByteArray())

        assertEquals(listOf("abcd", "efgh", "ij"), connection.writes.map { it.value.decodeToString() })
    }

    @Test
    fun writeWithoutResponseSupported_isPreferred() = runTest {
        val connection = connection(Write, WriteWithoutResponse)

        connection.writeChunked(RX, byteArrayOf(1))

        assertEquals(false, connection.writes.single().withResponse)
    }

    @Test
    fun onlyWriteSupported_writesWithResponse() = runTest {
        val connection = connection(Write)

        connection.writeChunked(RX, byteArrayOf(1))

        assertEquals(true, connection.writes.single().withResponse)
    }

    @Test
    fun emptyMessage_writesNothing() = runTest {
        val connection = connection(Write)

        assertTrue(connection.writeChunked(RX, ByteArray(0)).isSuccess)
        assertEquals(emptyList(), connection.writes)
    }

    @Test
    fun unknownCharacteristic_failsWithoutWriting() = runTest {
        val connection = connection(Write)
        val unknown = CharacteristicId(SERVICE, "6e400009-b5a3-f393-e0a9-e50e24dcca9e")

        val error = connection.writeChunked(unknown, byteArrayOf(1)).exceptionOrNull()

        assertIs<BluetoothException.CharacteristicNotFound>(error)
        assertEquals(emptyList(), connection.writes)
    }

    @Test
    fun notWritable_failsWithoutWriting() = runTest {
        val connection = connection(Notify)

        assertIs<BluetoothException.Platform>(connection.writeChunked(RX, byteArrayOf(1)).exceptionOrNull())
        assertEquals(emptyList(), connection.writes)
    }

    @Test
    fun linkDropped_returnsTheFailure() = runTest {
        val connection = connection(Write)
        connection.loseLink()

        assertIs<BluetoothException.NotConnected>(connection.writeChunked(RX, byteArrayOf(1)).exceptionOrNull())
    }

    private fun connection(vararg rxProperties: CharacteristicProperty, maxWriteLength: Int = 20) = FakeBleConnection(
        device = BleDevice(id = "1", name = "ESP32-NUS", rssi = -50),
        services = listOf(
            GattService(
                uuid = BluetoothUuid.normalize(SERVICE),
                characteristics = listOf(GattCharacteristic(RX.characteristic, rxProperties.toSet())),
            ),
        ),
        maxWriteLength = maxWriteLength,
    )

    private companion object {
        const val SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
        val RX = CharacteristicId(SERVICE, "6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    }
}
