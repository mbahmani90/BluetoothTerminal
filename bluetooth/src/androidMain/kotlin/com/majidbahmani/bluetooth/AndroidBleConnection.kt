package com.majidbahmani.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.os.Build
import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.BluetoothException
import com.majidbahmani.bluetooth.model.BluetoothUuid
import com.majidbahmani.bluetooth.model.CharacteristicId
import com.majidbahmani.bluetooth.model.CharacteristicProperty
import com.majidbahmani.bluetooth.model.ConnectionState
import com.majidbahmani.bluetooth.model.GattCharacteristic
import com.majidbahmani.bluetooth.model.GattService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds

/**
 * [BleConnection] on top of [BluetoothGatt].
 *
 * Created by [AndroidBleClient.connect], which calls [open] and only hands it out once it is connected.
 * Permissions are checked by the client before connecting (`AdapterState.On`).
 */
@SuppressLint("MissingPermission")
internal class AndroidBleConnection(
    private val context: Context,
    private val bluetoothDevice: BluetoothDevice,
    override val device: BleDevice,
) : BleConnection {

    /** Values for one characteristic, shared by all its collectors; [collectors] decides when to turn notifications on/off. */
    private class Subscription {
        val values = MutableSharedFlow<ByteArray>(extraBufferCapacity = 256, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        val collectors = AtomicInteger(0)
    }

    private val queue = GattQueue()
    private val connected = CompletableDeferred<Unit>()
    private val subscriptions = ConcurrentHashMap<CharacteristicId, Subscription>()
    private val mutableState = MutableStateFlow<ConnectionState>(ConnectionState.Connected)

    @Volatile private var gatt: BluetoothGatt? = null

    @Volatile override var services: List<GattService> = emptyList()
        private set

    @Volatile override var mtu: Int = DEFAULT_MTU
        private set

    override val maxWriteLength: Int get() = mtu - ATT_HEADER_SIZE

    override val state: StateFlow<ConnectionState> = mutableState.asStateFlow()

    /** Connects, asks for a bigger MTU and discovers services. On any failure or cancellation the GATT is closed. */
    suspend fun open(): Result<Unit> = try {
        // Deprecated in API 37 in favour of connectGatt(BluetoothGattConnectionSettings, Executor, callback),
        // which doesn't exist below 37. Still works there; switch with a version check once tested on a 37 device.
        @Suppress("DEPRECATION")
        gatt = bluetoothDevice.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
            ?: throw BluetoothException.Platform("connectGatt returned null")
        connected.await()
        // Optional: if the device refuses, writes stay limited to DEFAULT_MTU - 3 bytes.
        queue.enqueue<Int>("mtu", OPERATION_TIMEOUT) { gatt?.requestMtu(MAX_MTU) == true }
        queue.enqueue<Unit>("services", OPERATION_TIMEOUT) { gatt?.discoverServices() == true }
            .onFailure { closeGatt() }
    } catch (e: BluetoothException) {
        closeGatt()
        Result.failure(e)
    } catch (e: SecurityException) {
        closeGatt()
        Result.failure(BluetoothException.Platform("Missing Bluetooth permission", e))
    } catch (e: CancellationException) {
        closeGatt()
        throw e
    }

    override suspend fun read(characteristic: CharacteristicId): Result<ByteArray> {
        val (gatt, platformCharacteristic) = resolve(characteristic).getOrElse { return Result.failure(it) }
        return queue.enqueue("read", OPERATION_TIMEOUT) { gatt.readCharacteristic(platformCharacteristic) }
    }

    override suspend fun write(characteristic: CharacteristicId, value: ByteArray, withResponse: Boolean): Result<Unit> {
        if (value.size > maxWriteLength) {
            return Result.failure(
                BluetoothException.Platform("${value.size} bytes exceed maxWriteLength ($maxWriteLength)"),
            )
        }
        val (gatt, platformCharacteristic) = resolve(characteristic).getOrElse { return Result.failure(it) }
        val writeType = if (withResponse) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }
        return queue.enqueue("write", OPERATION_TIMEOUT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(platformCharacteristic, value, writeType) == BluetoothStatusCodes.SUCCESS
            } else {
                legacyWriteCharacteristic(gatt, platformCharacteristic, value, writeType)
            }
        }
    }

    override fun observe(characteristic: CharacteristicId): Flow<ByteArray> = channelFlow {
        val (_, platformCharacteristic) = resolve(characteristic).getOrThrow()
        val subscription = subscriptions.computeIfAbsent(characteristic) { Subscription() }

        // Subscribe before turning notifications on, so the first values aren't lost.
        val forwarding = launch(start = CoroutineStart.UNDISPATCHED) { subscription.values.collect { send(it) } }
        try {
            if (subscription.collectors.getAndIncrement() == 0) {
                setNotifications(platformCharacteristic, enable = true).getOrThrow()
            }
            // Ends the flow when the link drops; cancelling the collector ends it too.
            state.first { it is ConnectionState.Disconnected }
        } finally {
            forwarding.cancel()
            if (subscription.collectors.decrementAndGet() == 0 && state.value == ConnectionState.Connected) {
                withContext(NonCancellable) { setNotifications(platformCharacteristic, enable = false) }
            }
        }
    }

    override suspend fun disconnect() {
        markDisconnected(cause = null)
    }

    private fun resolve(id: CharacteristicId): Result<Pair<BluetoothGatt, BluetoothGattCharacteristic>> {
        val gatt = gatt
        if (gatt == null || state.value != ConnectionState.Connected) {
            return Result.failure(BluetoothException.NotConnected())
        }
        val platformCharacteristic = gatt.getService(UUID.fromString(id.service))
            ?.getCharacteristic(UUID.fromString(id.characteristic))
            ?: return Result.failure(BluetoothException.CharacteristicNotFound(id))
        return Result.success(gatt to platformCharacteristic)
    }

    /** setCharacteristicNotification only acts locally; most devices also need the CCCD (0x2902) written. */
    private suspend fun setNotifications(characteristic: BluetoothGattCharacteristic, enable: Boolean): Result<Unit> {
        val gatt = gatt ?: return Result.failure(BluetoothException.NotConnected())
        val descriptorValue = when {
            !enable -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            characteristic.has(BluetoothGattCharacteristic.PROPERTY_NOTIFY) ->
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            characteristic.has(BluetoothGattCharacteristic.PROPERTY_INDICATE) ->
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            else -> return Result.failure(
                BluetoothException.Platform("Characteristic ${characteristic.uuid} supports neither notify nor indicate"),
            )
        }
        if (!gatt.setCharacteristicNotification(characteristic, enable)) {
            return Result.failure(BluetoothException.OperationFailed("notify", GattQueue.NOT_STARTED))
        }
        // A few devices notify without a CCCD: nothing more to write then.
        val cccd = characteristic.getDescriptor(CCCD_UUID) ?: return Result.success(Unit)
        return queue.enqueue("descriptor", OPERATION_TIMEOUT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(cccd, descriptorValue) == BluetoothStatusCodes.SUCCESS
            } else {
                legacyWriteDescriptor(gatt, cccd, descriptorValue)
            }
        }
    }

    /** First caller wins: the app's disconnect (cause = null) or a lost link. */
    private fun markDisconnected(cause: BluetoothException?) {
        if (mutableState.compareAndSet(ConnectionState.Connected, ConnectionState.Disconnected(cause))) {
            queue.failPending(BluetoothException.NotConnected())
            closeGatt()
        }
    }

    /** close() is required: every unclosed GATT keeps one of the few client slots until Bluetooth restarts. */
    private fun closeGatt() {
        val gatt = gatt ?: return
        this.gatt = null
        runCatching {
            gatt.disconnect()
            gatt.close()
        }
    }

    private val callback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                connected.complete(Unit)
                return
            }
            // Still connecting → the connect fails; already connected → the link was lost.
            if (!connected.completeExceptionally(BluetoothException.OperationFailed("connect", status))) {
                markDisconnected(BluetoothException.OperationFailed("connection", status))
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                this@AndroidBleConnection.mtu = mtu
                queue.complete("mtu", mtu)
            } else {
                queue.fail("mtu", BluetoothException.OperationFailed("mtu", status))
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                services = gatt.services.map { it.toModel() }
                queue.complete("services", Unit)
            } else {
                queue.fail("services", BluetoothException.OperationFailed("services", status))
            }
        }

        // API 33+. Its default implementation calls the deprecated overload below, so only one of them runs.
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int,
        ) = onRead(value, status)

        @Deprecated("Called below API 33 only")
        @Suppress("DEPRECATION")
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) =
            onRead(characteristic.value ?: ByteArray(0), status)

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                queue.complete("write", Unit)
            } else {
                queue.fail("write", BluetoothException.OperationFailed("write", status))
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                queue.complete("descriptor", Unit)
            } else {
                queue.fail("descriptor", BluetoothException.OperationFailed("descriptor", status))
            }
        }

        // API 33+, same pattern as onCharacteristicRead.
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) = onNotification(characteristic, value)

        @Deprecated("Called below API 33 only")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) =
            onNotification(characteristic, characteristic.value ?: ByteArray(0))

        private fun onRead(value: ByteArray, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                queue.complete("read", value.copyOf())
            } else {
                queue.fail("read", BluetoothException.OperationFailed("read", status))
            }
        }

        private fun onNotification(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            val id = CharacteristicId(characteristic.service.uuid.toString(), characteristic.uuid.toString())
            // Copy: below API 33 the platform reuses the array for the next notification.
            subscriptions[id]?.values?.tryEmit(value.copyOf())
        }
    }

    private companion object {
        const val DEFAULT_MTU = 23
        const val MAX_MTU = 517
        const val ATT_HEADER_SIZE = 3
        val OPERATION_TIMEOUT = 10.seconds
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        fun BluetoothGattCharacteristic.has(property: Int): Boolean = properties and property != 0

        fun BluetoothGattService.toModel() = GattService(
            uuid = BluetoothUuid.normalize(uuid.toString()),
            characteristics = characteristics.map { characteristic ->
                GattCharacteristic(
                    uuid = BluetoothUuid.normalize(characteristic.uuid.toString()),
                    properties = PROPERTIES.filterKeys { characteristic.has(it) }.values.toSet(),
                )
            },
        )

        val PROPERTIES = mapOf(
            BluetoothGattCharacteristic.PROPERTY_READ to CharacteristicProperty.Read,
            BluetoothGattCharacteristic.PROPERTY_WRITE to CharacteristicProperty.Write,
            BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE to CharacteristicProperty.WriteWithoutResponse,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY to CharacteristicProperty.Notify,
            BluetoothGattCharacteristic.PROPERTY_INDICATE to CharacteristicProperty.Indicate,
        )

        // Below API 33 the value is set on the shared characteristic/descriptor object before writing.
        @Suppress("DEPRECATION")
        fun legacyWriteCharacteristic(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            writeType: Int,
        ): Boolean {
            characteristic.writeType = writeType
            characteristic.value = value
            return gatt.writeCharacteristic(characteristic)
        }

        @Suppress("DEPRECATION")
        fun legacyWriteDescriptor(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, value: ByteArray): Boolean {
            descriptor.value = value
            return gatt.writeDescriptor(descriptor)
        }
    }
}
