package com.majidbahmani.bluetoothterminal.feature.terminal.connection.data.repository

import com.majidbahmani.bluetooth.BleConnection
import com.majidbahmani.bluetooth.BluetoothSession
import com.majidbahmani.bluetooth.model.AdapterState
import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.BluetoothException
import com.majidbahmani.bluetooth.testing.FakeBleConnection
import com.majidbahmani.bluetooth.testing.FakeBluetoothClient
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class) // runCurrent()
class ConnectionRepositoryImplTest {

    private val client = FakeBluetoothClient()

    @Test
    fun status_nothingConnected_isDisconnected() = runTest {
        val (_, repository) = create()

        assertEquals(ConnectionStatus.Disconnected, repository.status.first())
    }

    @Test
    fun status_followsConnectingThenConnected() = runTest {
        val (session, repository) = create()
        val statuses = collect(repository)
        val pending = CompletableDeferred<Result<BleConnection>>()
        client.onConnect = { pending.await() }

        launch { session.connect(HM10) }
        runCurrent()
        pending.complete(Result.success(FakeBleConnection(HM10)))
        runCurrent()

        assertEquals(
            listOf(
                ConnectionStatus.Disconnected,
                ConnectionStatus.Connecting(HM10_INFO),
                ConnectionStatus.Connected(HM10_INFO),
            ),
            statuses,
        )
    }

    @Test
    fun status_connectTimesOut_isFailedTimeout() = runTest {
        val (session, repository) = create()
        client.onConnect = { Result.failure(BluetoothException.Timeout("connect")) }

        session.connect(HM10)

        assertEquals(ConnectionStatus.Failed(HM10_INFO, ConnectionError.Timeout), repository.status.first())
    }

    @Test
    fun status_bluetoothOff_isFailedBluetoothUnavailable() = runTest {
        val (session, repository) = create()
        client.adapterState.value = AdapterState.Off

        session.connect(HM10)

        assertEquals(
            ConnectionStatus.Failed(HM10_INFO, ConnectionError.BluetoothUnavailable),
            repository.status.first(),
        )
    }

    @Test
    fun status_otherError_isFailedUnknown() = runTest {
        val (session, repository) = create()
        client.onConnect = { Result.failure(BluetoothException.OperationFailed("connect", 133)) }

        session.connect(HM10)

        assertEquals(ConnectionStatus.Failed(HM10_INFO, ConnectionError.Unknown), repository.status.first())
    }

    @Test
    fun status_linkDrops_isLost() = runTest {
        val (session, repository) = create()
        val connection = session.connect(HM10).getOrThrow() as FakeBleConnection
        runCurrent()

        connection.loseLink()
        runCurrent()

        assertEquals(ConnectionStatus.Lost(HM10_INFO), repository.status.first())
    }

    @Test
    fun disconnect_connected_isDisconnected() = runTest {
        val (session, repository) = create()
        session.connect(HM10)
        runCurrent()

        repository.disconnect()
        runCurrent()

        assertEquals(ConnectionStatus.Disconnected, repository.status.first())
    }

    private fun TestScope.create(): Pair<BluetoothSession, ConnectionRepositoryImpl> {
        val session = BluetoothSession(client, scope = backgroundScope)
        return session to ConnectionRepositoryImpl(session)
    }

    private fun TestScope.collect(repository: ConnectionRepositoryImpl): List<ConnectionStatus> {
        val statuses = mutableListOf<ConnectionStatus>()
        backgroundScope.launch { repository.status.collect { statuses += it } }
        runCurrent()
        return statuses
    }

    private companion object {
        val HM10 = BleDevice(id = "AA:BB:CC:DD:EE:01", name = "HM-10", rssi = -60)
        val HM10_INFO = DeviceInfo(id = "AA:BB:CC:DD:EE:01", name = "HM-10")
    }
}
