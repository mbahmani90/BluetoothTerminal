package com.majidbahmani.bluetooth

import com.majidbahmani.bluetooth.model.BluetoothException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration

/**
 * Runs one GATT operation at a time.
 *
 * Android silently drops a GATT call while another one is in flight (the call returns false), so every
 * operation waits here until the previous one got its callback. The `BluetoothGattCallback` finishes the
 * running operation with [complete] or [fail].
 */
internal class GattQueue {

    private class Pending(val operation: String) {
        val result = CompletableDeferred<Any?>()
    }

    private val mutex = Mutex()
    private val pending = AtomicReference<Pending?>(null)

    /**
     * Calls [start] and suspends until the callback for [operation] arrives.
     *
     * [start] returns the platform call's result: false means Android didn't accept the operation.
     */
    suspend fun <T> enqueue(operation: String, timeout: Duration, start: () -> Boolean): Result<T> =
        mutex.withLock {
            val current = Pending(operation)
            pending.set(current)
            try {
                val started = try {
                    start()
                } catch (e: SecurityException) {
                    return@withLock Result.failure(BluetoothException.Platform("Missing Bluetooth permission", e))
                }
                if (!started) return@withLock Result.failure(BluetoothException.OperationFailed(operation, NOT_STARTED))

                @Suppress("UNCHECKED_CAST")
                Result.success(withTimeout(timeout) { current.result.await() } as T)
            } catch (e: TimeoutCancellationException) {
                Result.failure(BluetoothException.Timeout(operation))
            } catch (e: BluetoothException) {
                Result.failure(e)
            } finally {
                pending.compareAndSet(current, null)
            }
        }

    /** Finishes the running operation if it is [operation]; a late callback for an older one is ignored. */
    fun complete(operation: String, value: Any?) {
        pending.get()?.takeIf { it.operation == operation }?.result?.complete(value)
    }

    fun fail(operation: String, error: BluetoothException) {
        pending.get()?.takeIf { it.operation == operation }?.result?.completeExceptionally(error)
    }

    /** Fails whatever is running, e.g. when the link drops. */
    fun failPending(error: BluetoothException) {
        pending.get()?.result?.completeExceptionally(error)
    }

    companion object {
        /** Status used when Android refused to start the operation. */
        const val NOT_STARTED = -1
    }
}
