package com.majidbahmani.bluetooth.testing

import com.majidbahmani.bluetooth.BleConnection
import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.BluetoothException
import com.majidbahmani.bluetooth.model.CharacteristicId
import com.majidbahmani.bluetooth.model.ConnectionState
import com.majidbahmani.bluetooth.model.GattService
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** [BleConnection] for tests: records writes, serves reads, and lets the test push notifications or drop the link. */
public class FakeBleConnection(
    override val device: BleDevice,
    override val services: List<GattService> = emptyList(),
    override val maxWriteLength: Int = DEFAULT_MAX_WRITE_LENGTH,
) : BleConnection {

    public data class Write(val characteristic: CharacteristicId, val value: ByteArray, val withResponse: Boolean) {
        override fun equals(other: Any?): Boolean = other is Write &&
            characteristic == other.characteristic && value.contentEquals(other.value) && withResponse == other.withResponse

        override fun hashCode(): Int = (characteristic.hashCode() * 31 + value.contentHashCode()) * 31 + withResponse.hashCode()
    }

    override val state: MutableStateFlow<ConnectionState> = MutableStateFlow(ConnectionState.Connected)

    /** Values `read()` returns; a missing id fails with `CharacteristicNotFound`. */
    public val readValues: MutableMap<CharacteristicId, ByteArray> = mutableMapOf()

    private val mutableWrites = mutableListOf<Write>()
    private val notifications = MutableSharedFlow<Pair<CharacteristicId, ByteArray>>(extraBufferCapacity = 64)

    /** Every successful write, in order. */
    public val writes: List<Write> get() = mutableWrites

    /** The device sends [value] on [characteristic] (only active `observe()` collectors receive it). */
    public fun sendNotification(characteristic: CharacteristicId, value: ByteArray) {
        notifications.tryEmit(characteristic to value)
    }

    /** The link drops (out of range, device off, …). */
    public fun loseLink(cause: BluetoothException = BluetoothException.OperationFailed("connection", LINK_LOST_STATUS)) {
        state.value = ConnectionState.Disconnected(cause)
    }

    override suspend fun read(characteristic: CharacteristicId): Result<ByteArray> {
        if (state.value != ConnectionState.Connected) return Result.failure(BluetoothException.NotConnected())
        val value = readValues[characteristic]
            ?: return Result.failure(BluetoothException.CharacteristicNotFound(characteristic))
        return Result.success(value)
    }

    override suspend fun write(characteristic: CharacteristicId, value: ByteArray, withResponse: Boolean): Result<Unit> {
        if (state.value != ConnectionState.Connected) return Result.failure(BluetoothException.NotConnected())
        if (value.size > maxWriteLength) {
            return Result.failure(BluetoothException.Platform("${value.size} bytes exceed maxWriteLength ($maxWriteLength)"))
        }
        mutableWrites += Write(characteristic, value.copyOf(), withResponse)
        return Result.success(Unit)
    }

    /** Like the real one: values for [characteristic] until the link drops. */
    override fun observe(characteristic: CharacteristicId): Flow<ByteArray> = channelFlow {
        val forwarding = launch(start = CoroutineStart.UNDISPATCHED) {
            notifications.collect { (id, value) -> if (id == characteristic) send(value) }
        }
        state.first { it is ConnectionState.Disconnected }
        forwarding.cancel()
    }

    override suspend fun disconnect() {
        if (state.value == ConnectionState.Connected) state.value = ConnectionState.Disconnected(cause = null)
    }

    private companion object {
        const val DEFAULT_MAX_WRITE_LENGTH = 20

        /** Android's "connection timeout" status, a typical lost-link code. */
        const val LINK_LOST_STATUS = 8
    }
}
