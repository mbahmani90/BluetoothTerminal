package com.majidbahmani.bluetooth

import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.CharacteristicId
import com.majidbahmani.bluetooth.model.ConnectionState
import com.majidbahmani.bluetooth.model.GattService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * An open connection to one device, returned by [BluetoothClient.connect].
 *
 * Once [state] is [ConnectionState.Disconnected] the connection is finished: connect again to get a new one.
 */
public interface BleConnection {

    public val device: BleDevice

    public val state: StateFlow<ConnectionState>

    /** Services discovered while connecting. */
    public val services: List<GattService>

    /** Max bytes in one write: negotiated MTU - 3 on Android, `maximumWriteValueLength` on iOS. */
    public val maxWriteLength: Int

    public suspend fun read(characteristic: CharacteristicId): Result<ByteArray>

    /**
     * Writes [value] (at most [maxWriteLength] bytes).
     *
     * [withResponse] = true waits for the device to acknowledge; false is faster but may be dropped.
     */
    public suspend fun write(characteristic: CharacteristicId, value: ByteArray, withResponse: Boolean): Result<Unit>

    /**
     * Values the device sends for [characteristic] (notify or indicate).
     *
     * Cold: collecting enables notifications, cancelling disables them. One emission per notification;
     * a message from the device may be split over several emissions.
     */
    public fun observe(characteristic: CharacteristicId): Flow<ByteArray>

    public suspend fun disconnect()
}
