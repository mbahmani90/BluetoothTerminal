package com.majidbahmani.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import com.majidbahmani.bluetooth.model.AdapterState
import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.BluetoothException
import com.majidbahmani.bluetooth.model.BluetoothUuid
import com.majidbahmani.bluetooth.model.ScanFilter
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import android.bluetooth.le.ScanFilter as PlatformScanFilter

/**
 * [BluetoothClient] for Android: `BluetoothLeScanner` for scanning, [AndroidBleConnection] for GATT.
 *
 * Every Bluetooth call is guarded by [AdapterState.On], which includes the runtime permissions; the
 * "MissingPermission" lint check can't see that, hence the suppression.
 */
@SuppressLint("MissingPermission")
internal class AndroidBleClient(private val context: Context) : BluetoothClient {

    private val adapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val mutableAdapterState = MutableStateFlow(currentAdapterState())

    override val adapterState: StateFlow<AdapterState> = mutableAdapterState.asStateFlow()

    init {
        // Bluetooth switched on/off (also from quick settings).
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = refreshAdapterState()
        }
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        // A granted permission has no broadcast: re-check whenever an activity resumes
        // (after the permission dialog, or coming back from Settings).
        (context as? Application)?.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = refreshAdapterState()
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    override fun scan(filter: ScanFilter): Flow<List<BleDevice>> = callbackFlow {
        refreshAdapterState()
        val state = adapterState.value
        val scanner = adapter?.bluetoothLeScanner
        if (state != AdapterState.On || scanner == null) throw BluetoothException.NotAvailable(state)

        val devices = LinkedHashMap<String, BleDevice>()   // by id, in discovery order
        fun onResult(result: ScanResult) {
            val record = result.scanRecord
            val name = record?.deviceName ?: result.device.name
            if (filter.namePrefix != null && name?.startsWith(filter.namePrefix, ignoreCase = true) != true) return
            devices[result.device.address] = BleDevice(
                id = result.device.address,
                name = name,
                rssi = result.rssi,
                advertisedServices = record?.serviceUuids.orEmpty().map { BluetoothUuid.normalize(it.toString()) },
            )
            trySend(devices.values.toList())
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) = onResult(result)
            override fun onBatchScanResults(results: MutableList<ScanResult>) = results.forEach(::onResult)
            override fun onScanFailed(errorCode: Int) {
                close(BluetoothException.OperationFailed("scan", errorCode))
            }
        }

        // Android filters by service in hardware (one filter per UUID = OR); the name prefix is checked above.
        val platformFilters = filter.serviceUuids.map { uuid ->
            PlatformScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(BluetoothUuid.normalize(uuid))).build()
        }
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

        // The scanner stops silently when Bluetooth is switched off: end the flow with the reason.
        launch {
            val newState = adapterState.first { it != AdapterState.On }
            close(BluetoothException.NotAvailable(newState))
        }

        trySend(emptyList())
        scanner.startScan(platformFilters, settings, callback)
        awaitClose {
            // Throws if Bluetooth is already off; nothing to stop then.
            runCatching { scanner.stopScan(callback) }
        }
    }.conflate() // Each emission is the full list: a slow collector only needs the latest one.

    override suspend fun connect(device: BleDevice, timeout: Duration): Result<BleConnection> {
        refreshAdapterState()
        val state = adapterState.value
        if (state != AdapterState.On) return Result.failure(BluetoothException.NotAvailable(state))

        val bluetoothDevice = try {
            adapter!!.getRemoteDevice(device.id)
        } catch (e: IllegalArgumentException) {
            return Result.failure(BluetoothException.Platform("Invalid device id: ${device.id}", e))
        }

        return try {
            withTimeout(timeout) {
                val first = AndroidBleConnection(context, bluetoothDevice, device)
                val result = first.open()
                val error = result.exceptionOrNull()
                // Status 133 (GATT_ERROR) is a common, often transient Android stack error: retry once.
                if (error is BluetoothException.OperationFailed && error.status == GATT_ERROR) {
                    delay(RETRY_DELAY)
                    val second = AndroidBleConnection(context, bluetoothDevice, device)
                    second.open().map { second }
                } else {
                    result.map { first }
                }
            }
        } catch (e: TimeoutCancellationException) {
            // open() closed its GATT on cancellation.
            Result.failure(BluetoothException.Timeout("connect"))
        }
    }

    private fun refreshAdapterState() {
        mutableAdapterState.value = currentAdapterState()
    }

    private fun currentAdapterState(): AdapterState = when {
        adapter == null || !context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) ->
            AdapterState.Unsupported
        !hasPermission(Manifest.permission.BLUETOOTH_SCAN) || !hasPermission(Manifest.permission.BLUETOOTH_CONNECT) ->
            AdapterState.Unauthorized
        adapter.isEnabled -> AdapterState.On
        else -> AdapterState.Off
    }

    private fun hasPermission(permission: String): Boolean =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private companion object {
        /** Not a public constant in the SDK. */
        const val GATT_ERROR = 133
        val RETRY_DELAY = 500.milliseconds
    }
}
