package com.majidbahmani.bluetoothterminal.feature.terminal.connection.data.repository

import com.majidbahmani.bluetooth.BluetoothSession
import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.BluetoothUuid
import com.majidbahmani.bluetooth.model.CharacteristicId
import com.majidbahmani.bluetooth.model.CharacteristicProperty
import com.majidbahmani.bluetooth.model.CharacteristicProperty.Notify
import com.majidbahmani.bluetooth.model.CharacteristicProperty.Read
import com.majidbahmani.bluetooth.model.CharacteristicProperty.Write
import com.majidbahmani.bluetooth.model.CharacteristicProperty.WriteWithoutResponse
import com.majidbahmani.bluetooth.model.GattCharacteristic
import com.majidbahmani.bluetooth.model.GattService
import com.majidbahmani.bluetooth.testing.FakeBleConnection
import com.majidbahmani.bluetooth.testing.FakeBluetoothClient
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelException
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelSelection
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceCharacteristic
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.IncomingData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class) // runCurrent()
class ChannelRepositoryImplTest {

    private val client = FakeBluetoothClient()

    @Test
    fun notConnected_isEmptyAndCantSend() = runTest {
        val (_, repository) = create()

        assertEquals(emptyList(), repository.services.first())
        assertEquals(ChannelSelection(), repository.selection.first())
        assertEquals(ChannelError.NotConnected, repository.send(byteArrayOf(1)).channelError())
    }

    @Test
    fun services_mapsWhatEachCharacteristicSupports() = runTest {
        val (_, repository) = createConnected(NUS)

        val characteristics = repository.services.first().single().characteristics

        assertEquals(
            listOf(
                DeviceCharacteristic(RX, canRead = false, canWrite = true, canNotify = false),
                DeviceCharacteristic(TX, canRead = false, canWrite = false, canNotify = true),
                DeviceCharacteristic(VERSION, canRead = true, canWrite = false, canNotify = false),
            ),
            characteristics,
        )
    }

    @Test
    fun connected_selectionIsDetectedDefault() = runTest {
        val (_, repository) = createConnected(NUS)

        assertEquals(ChannelSelection(sendTarget = RX, listening = setOf(TX)), repository.selection.first())
    }

    @Test
    fun connected_noSerialChannel_selectsNothingAndCantSend() = runTest {
        val battery = service("180f", characteristic("2a19", Read, Notify))
        val (_, repository) = createConnected(battery)

        assertEquals(ChannelSelection(), repository.selection.first())
        assertEquals(ChannelError.NoSendTarget, repository.send(byteArrayOf(1)).channelError())
    }

    @Test
    fun setSendTarget_writable_isUsedForSending() = runTest {
        val (connection, repository) = createConnected(NUS, CUSTOM)

        repository.setSendTarget(CUSTOM_RX)
        repository.send("hi".encodeToByteArray())

        assertEquals(CUSTOM_RX, repository.selection.first().sendTarget)
        assertEquals(CharacteristicId(CUSTOM_SERVICE, CUSTOM_RX.characteristic), connection.writes.single().characteristic)
    }

    @Test
    fun setSendTarget_notWritable_isIgnored() = runTest {
        val (_, repository) = createConnected(NUS)

        repository.setSendTarget(TX)

        assertEquals(RX, repository.selection.first().sendTarget)
    }

    @Test
    fun setListening_addsAndRemoves_onlyNotifyingCharacteristics() = runTest {
        val (_, repository) = createConnected(NUS, CUSTOM)

        repository.setListening(CUSTOM_TX, listening = true)
        repository.setListening(TX, listening = false)
        repository.setListening(VERSION, listening = true) // can't notify: ignored

        assertEquals(setOf(CUSTOM_TX), repository.selection.first().listening)
    }

    @Test
    fun incoming_isTaggedWithItsCharacteristic_andFollowsListening() = runTest {
        val (connection, repository) = createConnected(NUS, CUSTOM)
        repository.setListening(CUSTOM_TX, listening = true)
        val received = collectIncoming(repository)

        connection.sendNotification(TX.toId(), "tick 1".encodeToByteArray())
        connection.sendNotification(CUSTOM_TX.toId(), "42".encodeToByteArray())
        runCurrent()
        repository.setListening(TX, listening = false)
        runCurrent()
        connection.sendNotification(TX.toId(), "tick 2".encodeToByteArray())
        runCurrent()

        assertEquals(
            listOf(IncomingData(TX, "tick 1".encodeToByteArray()), IncomingData(CUSTOM_TX, "42".encodeToByteArray())),
            received,
        )
    }

    @Test
    fun send_longMessage_isSplitToMaxWriteLength() = runTest {
        val (connection, repository) = createConnected(NUS, maxWriteLength = 4)

        val result = repository.send("hello world".encodeToByteArray())

        assertTrue(result.isSuccess)
        assertEquals(listOf("hell", "o wo", "rld"), connection.writes.map { it.value.decodeToString() })
    }

    @Test
    fun read_readable_returnsValue() = runTest {
        val (connection, repository) = createConnected(NUS)
        connection.readValues[VERSION.toId()] = "1.0".encodeToByteArray()

        assertContentEquals("1.0".encodeToByteArray(), repository.read(VERSION).getOrThrow())
    }

    @Test
    fun read_notReadable_isNotSupported() = runTest {
        val (_, repository) = createConnected(NUS)

        assertEquals(ChannelError.NotSupported, repository.read(RX).channelError())
    }

    @Test
    fun newConnection_startsFromItsDefaultAgain() = runTest {
        val (session, repository) = create(NUS)
        session.connect(DEVICE)
        runCurrent()
        repository.setSendTarget(null)
        assertEquals(null, repository.selection.first().sendTarget)

        session.connect(DEVICE)
        runCurrent()

        assertEquals(ChannelSelection(sendTarget = RX, listening = setOf(TX)), repository.selection.first())
    }

    @Test
    fun linkLost_isEmptyAgain() = runTest {
        val (connection, repository) = createConnected(NUS)

        connection.loseLink()
        runCurrent()

        assertEquals(emptyList(), repository.services.first())
        assertEquals(ChannelSelection(), repository.selection.first())
    }

    private fun TestScope.create(
        vararg services: GattService,
        maxWriteLength: Int = 20,
    ): Pair<BluetoothSession, ChannelRepositoryImpl> {
        client.onConnect = { Result.success(FakeBleConnection(it, services.toList(), maxWriteLength = maxWriteLength)) }
        val session = BluetoothSession(client, scope = backgroundScope)
        return session to ChannelRepositoryImpl(session)
    }

    private suspend fun TestScope.createConnected(
        vararg services: GattService,
        maxWriteLength: Int = 20,
    ): Pair<FakeBleConnection, ChannelRepositoryImpl> {
        val (session, repository) = create(*services, maxWriteLength = maxWriteLength)
        val connection = session.connect(DEVICE).getOrThrow() as FakeBleConnection
        runCurrent()
        return connection to repository
    }

    private fun TestScope.collectIncoming(repository: ChannelRepositoryImpl): List<IncomingData> {
        val received = mutableListOf<IncomingData>()
        backgroundScope.launch { repository.incoming.collect { received += it } }
        runCurrent()
        return received
    }

    private fun Result<*>.channelError(): ChannelError? = (exceptionOrNull() as? ChannelException)?.error

    private companion object {
        val DEVICE = BleDevice(id = "1", name = "ESP32-NUS", rssi = -40)

        val NUS_SERVICE = BluetoothUuid.normalize("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val RX = CharacteristicRef(NUS_SERVICE, "6e400002-b5a3-f393-e0a9-e50e24dcca9e")
        val TX = CharacteristicRef(NUS_SERVICE, "6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val VERSION = CharacteristicRef(NUS_SERVICE, "6e400004-b5a3-f393-e0a9-e50e24dcca9e")
        val NUS = service(
            NUS_SERVICE,
            characteristic(RX.characteristic, Write, WriteWithoutResponse),
            characteristic(TX.characteristic, Notify),
            characteristic(VERSION.characteristic, Read),
        )

        const val CUSTOM_SERVICE = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
        val CUSTOM_RX = CharacteristicRef(CUSTOM_SERVICE, "beb5483e-36e1-4688-b7f5-ea07361b26a8")
        val CUSTOM_TX = CharacteristicRef(CUSTOM_SERVICE, "beb5483f-36e1-4688-b7f5-ea07361b26a8")
        val CUSTOM = service(
            CUSTOM_SERVICE,
            characteristic(CUSTOM_RX.characteristic, Write),
            characteristic(CUSTOM_TX.characteristic, Notify),
        )

        fun service(uuid: String, vararg characteristics: GattCharacteristic) =
            GattService(BluetoothUuid.normalize(uuid), characteristics.toList())

        fun characteristic(uuid: String, vararg properties: CharacteristicProperty) =
            GattCharacteristic(BluetoothUuid.normalize(uuid), properties.toSet())

        fun CharacteristicRef.toId() = CharacteristicId(service, characteristic)
    }
}
