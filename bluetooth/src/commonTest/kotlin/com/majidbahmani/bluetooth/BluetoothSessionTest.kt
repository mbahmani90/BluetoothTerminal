package com.majidbahmani.bluetooth

import com.majidbahmani.bluetooth.model.AdapterState
import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.BluetoothException
import com.majidbahmani.bluetooth.model.ConnectionState
import com.majidbahmani.bluetooth.model.SessionState
import com.majidbahmani.bluetooth.testing.FakeBleConnection
import com.majidbahmani.bluetooth.testing.FakeBluetoothClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class) // runCurrent()
class BluetoothSessionTest {

    private val client = FakeBluetoothClient()

    @Test
    fun initialState_isIdleWithoutConnection() = runTest {
        val session = session()

        assertEquals(SessionState.Idle, session.state.value)
        assertNull(session.connection)
    }

    @Test
    fun connect_success_isConnectedWithThatConnection() = runTest {
        val session = session()

        val connection = session.connect(HM10).getOrThrow()

        assertEquals(SessionState.Connected(connection), session.state.value)
        assertSame(connection, session.connection)
        assertEquals(listOf(HM10), client.connectRequests)
    }

    @Test
    fun connect_whileWaiting_isConnecting() = runTest {
        val session = session()
        val pending = CompletableDeferred<Result<BleConnection>>()
        client.onConnect = { pending.await() }

        val connecting = async { session.connect(HM10) }
        runCurrent()
        assertEquals(SessionState.Connecting(HM10), session.state.value)

        pending.complete(Result.success(FakeBleConnection(HM10)))
        connecting.await()
        assertIs<SessionState.Connected>(session.state.value)
    }

    @Test
    fun connect_failure_isDisconnectedWithCause() = runTest {
        val session = session()
        val timeout = BluetoothException.Timeout("connect")
        client.onConnect = { Result.failure(timeout) }

        val result = session.connect(HM10)

        assertSame(timeout, result.exceptionOrNull())
        assertEquals(SessionState.Disconnected(HM10, timeout), session.state.value)
        assertNull(session.connection)
    }

    @Test
    fun connect_bluetoothOff_isDisconnectedNotAvailable() = runTest {
        val session = session()
        client.adapterState.value = AdapterState.Off

        session.connect(HM10)

        val state = assertIs<SessionState.Disconnected>(session.state.value)
        assertIs<BluetoothException.NotAvailable>(state.cause)
    }

    @Test
    fun connect_otherDevice_disconnectsCurrentFirst() = runTest {
        val session = session()
        val first = session.connect(HM10).getOrThrow() as FakeBleConnection

        val second = session.connect(NORDIC).getOrThrow()

        assertEquals(ConnectionState.Disconnected(cause = null), first.state.value)
        assertEquals(SessionState.Connected(second), session.state.value)
    }

    @Test
    fun linkLost_isDisconnectedWithCause() = runTest {
        val session = session()
        val connection = session.connect(HM10).getOrThrow() as FakeBleConnection
        runCurrent()

        val cause = BluetoothException.OperationFailed("connection", 8)
        connection.loseLink(cause)
        runCurrent()

        assertEquals(SessionState.Disconnected(HM10, cause), session.state.value)
        assertNull(session.connection)
    }

    @Test
    fun disconnect_closesConnectionAndIsIdle() = runTest {
        val session = session()
        val connection = session.connect(HM10).getOrThrow() as FakeBleConnection
        runCurrent()

        session.disconnect()
        runCurrent()

        assertEquals(ConnectionState.Disconnected(cause = null), connection.state.value)
        // The app's own disconnect is not reported as a lost link.
        assertEquals(SessionState.Idle, session.state.value)
    }

    @Test
    fun disconnect_afterFailure_clearsError() = runTest {
        val session = session()
        client.onConnect = { Result.failure(BluetoothException.Timeout("connect")) }
        session.connect(HM10)

        session.disconnect()

        assertEquals(SessionState.Idle, session.state.value)
    }

    @Test
    fun connect_cancelled_isIdle() = runTest {
        val session = session()
        client.onConnect = { CompletableDeferred<Result<BleConnection>>().await() }

        val connecting = async { session.connect(HM10) }
        runCurrent()
        connecting.cancel()
        runCurrent()

        assertEquals(SessionState.Idle, session.state.value)
    }

    private fun TestScope.session() = BluetoothSession(client, scope = backgroundScope)

    private companion object {
        val HM10 = BleDevice(id = "AA:BB:CC:DD:EE:01", name = "HM-10", rssi = -60)
        val NORDIC = BleDevice(id = "AA:BB:CC:DD:EE:02", name = "Nordic UART", rssi = -70)
    }
}
