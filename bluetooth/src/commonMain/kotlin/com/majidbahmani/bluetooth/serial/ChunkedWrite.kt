package com.majidbahmani.bluetooth.serial

import com.majidbahmani.bluetooth.BleConnection
import com.majidbahmani.bluetooth.model.BluetoothException
import com.majidbahmani.bluetooth.model.CharacteristicId
import com.majidbahmani.bluetooth.model.CharacteristicProperty
import com.majidbahmani.bluetooth.model.find

/**
 * Writes [bytes] of any length to [characteristic], split into chunks of at most
 * [BleConnection.maxWriteLength] bytes, sent in order.
 *
 * - Uses write without response when the characteristic supports it (faster; HM-10 only offers it),
 *   otherwise write with response.
 * - Stops at the first chunk that fails and returns that failure; earlier chunks were already sent.
 * - Fails with [BluetoothException.CharacteristicNotFound] if the device has no such characteristic, or
 *   [BluetoothException.Platform] if it can't be written. Empty [bytes] writes nothing.
 */
public suspend fun BleConnection.writeChunked(characteristic: CharacteristicId, bytes: ByteArray): Result<Unit> {
    val target = services.find(characteristic)
        ?: return Result.failure(BluetoothException.CharacteristicNotFound(characteristic))
    if (!target.canWrite()) {
        return Result.failure(BluetoothException.Platform("Characteristic can't be written: $characteristic"))
    }
    val withResponse = CharacteristicProperty.WriteWithoutResponse !in target.properties

    for (start in bytes.indices step maxWriteLength) {
        val chunk = bytes.copyOfRange(start, minOf(start + maxWriteLength, bytes.size))
        write(characteristic, chunk, withResponse).onFailure { return Result.failure(it) }
    }
    return Result.success(Unit)
}
