package com.majidbahmani.bluetooth.model

/**
 * Every failure the library reports, as `Result.failure(BluetoothException)`.
 *
 * Sealed, so the UI can map each case to its own message with an exhaustive `when`.
 */
public sealed class BluetoothException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** Bluetooth can't be used right now; [state] says why (off, unauthorized, unsupported). */
    public class NotAvailable(public val state: AdapterState) : BluetoothException("Bluetooth not available: $state")

    /** [operation] (e.g. "connect", "write") got no answer in time. */
    public class Timeout(public val operation: String) : BluetoothException("Timed out: $operation")

    /** The connection was already closed. */
    public class NotConnected : BluetoothException("Not connected")

    /** The device has no such service/characteristic. */
    public class CharacteristicNotFound(public val id: CharacteristicId) :
        BluetoothException("Characteristic not found: $id")

    /** The platform rejected [operation] with a status code (Android GATT status, iOS CBError code). */
    public class OperationFailed(public val operation: String, public val status: Int) :
        BluetoothException("$operation failed with status $status")

    /** Any other platform error. */
    public class Platform(message: String, cause: Throwable? = null) : BluetoothException(message, cause)
}
