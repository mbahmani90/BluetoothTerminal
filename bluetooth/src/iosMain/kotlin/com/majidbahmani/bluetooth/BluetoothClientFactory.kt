package com.majidbahmani.bluetooth

import com.majidbahmani.bluetooth.model.AdapterState
import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.BluetoothException
import com.majidbahmani.bluetooth.model.ScanFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

/**
 * Creates the iOS [BluetoothClient].
 *
 * Placeholder until the CoreBluetooth implementation (`IosBleClient`) lands: reports
 * [AdapterState.Unsupported], so the iOS app runs and shows "not supported" instead of crashing.
 */
public fun createBluetoothClient(): BluetoothClient = UnsupportedBluetoothClient

private object UnsupportedBluetoothClient : BluetoothClient {

    override val adapterState: StateFlow<AdapterState> = MutableStateFlow(AdapterState.Unsupported)

    override fun scan(filter: ScanFilter): Flow<List<BleDevice>> = flow {
        throw BluetoothException.NotAvailable(AdapterState.Unsupported)
    }

    override suspend fun connect(device: BleDevice, timeout: Duration): Result<BleConnection> =
        Result.failure(BluetoothException.NotAvailable(AdapterState.Unsupported))
}
