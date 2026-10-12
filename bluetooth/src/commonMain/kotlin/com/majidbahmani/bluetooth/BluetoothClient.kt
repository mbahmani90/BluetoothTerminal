package com.majidbahmani.bluetooth

import com.majidbahmani.bluetooth.model.AdapterState
import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.ScanFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Entry point to BLE: adapter state, scanning and connecting.
 *
 * Implemented per platform (Android `BluetoothGatt`, iOS CoreBluetooth); common code, view models
 * and tests only depend on this interface.
 */
public interface BluetoothClient {

    /** Whether BLE can be used right now. Updates when Bluetooth is toggled or the permission changes. */
    public val adapterState: StateFlow<AdapterState>

    /**
     * Scans for advertising devices.
     *
     * Cold: collecting starts the scan, cancelling the collector stops it. Emits every device found
     * so far, once per [BleDevice.id], in discovery order; name and RSSI are updated in place.
     * Fails with [com.majidbahmani.bluetooth.model.BluetoothException.NotAvailable] when
     * [adapterState] is not [AdapterState.On].
     */
    public fun scan(filter: ScanFilter = ScanFilter()): Flow<List<BleDevice>>

    /**
     * Connects to [device] and discovers its services before returning, so the connection is ready to use.
     *
     * Fails with a [com.majidbahmani.bluetooth.model.BluetoothException] (e.g. `Timeout`); never hangs
     * longer than [timeout].
     */
    public suspend fun connect(device: BleDevice, timeout: Duration = 15.seconds): Result<BleConnection>
}
