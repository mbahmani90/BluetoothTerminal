package com.majidbahmani.bluetooth

import com.majidbahmani.bluetooth.model.BluetoothException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class GattQueueTest {

    private val queue = GattQueue()

    @Test
    fun enqueue_callbackCompletes_returnsValue() = runTest {
        val read = async { queue.enqueue<ByteArray>("read", TIMEOUT) { true } }
        runCurrent()

        queue.complete("read", byteArrayOf(1, 2))

        assertContentEquals(byteArrayOf(1, 2), read.await().getOrThrow())
    }

    @Test
    fun enqueue_secondOperationStartsOnlyAfterFirstCompletes() = runTest {
        val started = mutableListOf<String>()
        val first = async { queue.enqueue<Unit>("write", TIMEOUT) { started += "write"; true } }
        val second = async { queue.enqueue<ByteArray>("read", TIMEOUT) { started += "read"; true } }
        runCurrent()
        assertEquals(listOf("write"), started)

        queue.complete("write", Unit)
        runCurrent()
        assertEquals(listOf("write", "read"), started)

        queue.complete("read", byteArrayOf())
        assertTrue(first.await().isSuccess)
        assertTrue(second.await().isSuccess)
    }

    @Test
    fun enqueue_platformRefuses_failsWithoutWaiting() = runTest {
        val error = queue.enqueue<Unit>("write", TIMEOUT) { false }.exceptionOrNull()

        assertIs<BluetoothException.OperationFailed>(error)
        assertEquals(GattQueue.NOT_STARTED, error.status)
    }

    @Test
    fun enqueue_missingPermission_failsWithPlatformError() = runTest {
        val error = queue.enqueue<Unit>("write", TIMEOUT) { throw SecurityException("no permission") }.exceptionOrNull()

        assertIs<BluetoothException.Platform>(error)
    }

    @Test
    fun enqueue_noCallback_timesOut() = runTest {
        val read = async { queue.enqueue<ByteArray>("read", TIMEOUT) { true } }

        advanceTimeBy(TIMEOUT + 1.seconds)

        val error = read.await().exceptionOrNull()
        assertIs<BluetoothException.Timeout>(error)
        assertEquals("read", error.operation)
    }

    @Test
    fun fail_errorStatus_isReturned() = runTest {
        val write = async { queue.enqueue<Unit>("write", TIMEOUT) { true } }
        runCurrent()

        queue.fail("write", BluetoothException.OperationFailed("write", 3))

        val error = write.await().exceptionOrNull()
        assertIs<BluetoothException.OperationFailed>(error)
        assertEquals(3, error.status)
    }

    @Test
    fun complete_otherOperation_isIgnored() = runTest {
        val read = async { queue.enqueue<ByteArray>("read", TIMEOUT) { true } }
        runCurrent()

        queue.complete("write", Unit) // late callback of an earlier write
        runCurrent()
        assertFalse(read.isCompleted)

        queue.complete("read", byteArrayOf(7))
        assertContentEquals(byteArrayOf(7), read.await().getOrThrow())
    }

    @Test
    fun failPending_linkLost_failsRunningOperation() = runTest {
        val write = async { queue.enqueue<Unit>("write", TIMEOUT) { true } }
        runCurrent()

        queue.failPending(BluetoothException.NotConnected())

        assertIs<BluetoothException.NotConnected>(write.await().exceptionOrNull())
    }

    private companion object {
        val TIMEOUT = 10.seconds
    }
}
