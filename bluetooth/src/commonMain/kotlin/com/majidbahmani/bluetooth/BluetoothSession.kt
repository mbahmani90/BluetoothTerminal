package com.majidbahmani.bluetooth

import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.BluetoothException
import com.majidbahmani.bluetooth.model.ConnectionState
import com.majidbahmani.bluetooth.model.SessionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Holds the one active connection of an app that talks to one device at a time.
 *
 * Create one per app (e.g. a DI singleton) and share it: one screen connects, others use [connection].
 * Optional: apps with several devices use [BluetoothClient] directly.
 *
 * @param scope lives as long as the session (app scope); used to notice when the link drops.
 */
public class BluetoothSession(
    private val client: BluetoothClient,
    private val scope: CoroutineScope,
) {

    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Idle)
    private var linkWatcher: Job? = null

    public val state: StateFlow<SessionState> = mutableState.asStateFlow()

    /** The active connection, or null when not [SessionState.Connected]. */
    public val connection: BleConnection?
        get() = (state.value as? SessionState.Connected)?.connection

    /**
     * Connects to [device], disconnecting the current device first.
     *
     * Calls are handled one after another. Cancelling the caller cancels the attempt (state → [SessionState.Idle]).
     * A failed attempt ends in [SessionState.Failed]; a link that drops later in [SessionState.Lost].
     */
    public suspend fun connect(device: BleDevice, timeout: Duration = 15.seconds): Result<BleConnection> =
        mutex.withLock {
            closeCurrent()
            mutableState.value = SessionState.Connecting(device)
            val result = try {
                client.connect(device, timeout)
            } catch (e: CancellationException) {
                mutableState.value = SessionState.Idle
                throw e
            }
            result
                .onSuccess { connection ->
                    mutableState.value = SessionState.Connected(connection)
                    watchLink(connection)
                }
                .onFailure { error ->
                    mutableState.value = SessionState.Failed(device, error.asBluetoothException())
                }
        }

    /** Disconnects the current device (if any) and clears a previous error: state → [SessionState.Idle]. */
    public suspend fun disconnect() {
        mutex.withLock {
            closeCurrent()
            mutableState.value = SessionState.Idle
        }
    }

    /** Stops watching first, so the app's own disconnect isn't reported as lost. */
    private suspend fun closeCurrent() {
        linkWatcher?.cancel()
        linkWatcher = null
        connection?.disconnect()
    }

    private fun watchLink(connection: BleConnection) {
        linkWatcher = scope.launch {
            val lost = connection.state.first { it is ConnectionState.Disconnected } as ConnectionState.Disconnected
            val cause = lost.cause ?: BluetoothException.NotConnected()
            // Only if this connection is still the current one.
            mutableState.compareAndSet(
                SessionState.Connected(connection),
                SessionState.Lost(connection.device, cause),
            )
        }
    }

    private fun Throwable.asBluetoothException(): BluetoothException =
        this as? BluetoothException ?: BluetoothException.Platform(message ?: "Connection failed", this)
}
