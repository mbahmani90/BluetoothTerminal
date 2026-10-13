package com.majidbahmani.bluetooth.testing

import com.majidbahmani.bluetooth.BleConnection
import com.majidbahmani.bluetooth.BluetoothClient
import com.majidbahmani.bluetooth.model.AdapterState
import com.majidbahmani.bluetooth.model.BleDevice
import com.majidbahmani.bluetooth.model.BluetoothException
import com.majidbahmani.bluetooth.model.BluetoothUuid
import com.majidbahmani.bluetooth.model.ScanFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

/**
 * [BluetoothClient] for tests and previews: no hardware, everything controlled by the test.
 *
 * In `commonMain` (not `commonTest`) so other modules' tests can use it: KMP has no test fixtures.
 */
public class FakeBluetoothClient(initialState: AdapterState = AdapterState.On) : BluetoothClient {

    /** Set it to simulate Bluetooth off, a missing permission, … */
    override val adapterState: MutableStateFlow<AdapterState> = MutableStateFlow(initialState)

    /** Devices "in range": `scan()` emits them, filtered like a real scan. */
    public val devicesInRange: MutableStateFlow<List<BleDevice>> = MutableStateFlow(emptyList())

    /** What `connect()` returns. Default: a new connected [FakeBleConnection]. Can suspend to test "connecting". */
    public var onConnect: suspend (BleDevice) -> Result<BleConnection> = { Result.success(FakeBleConnection(it)) }

    private val mutableConnectRequests = mutableListOf<BleDevice>()

    /** Every device `connect()` was called with, in order. */
    public val connectRequests: List<BleDevice> get() = mutableConnectRequests

    override fun scan(filter: ScanFilter): Flow<List<BleDevice>> = flow {
        val state = adapterState.value
        if (state != AdapterState.On) throw BluetoothException.NotAvailable(state)
        val services = filter.serviceUuids.map(BluetoothUuid::normalize)
        devicesInRange
            .map { devices -> devices.filter { it.matches(services, filter.namePrefix) } }
            .collect { emit(it) }
    }

    override suspend fun connect(device: BleDevice, timeout: Duration): Result<BleConnection> {
        mutableConnectRequests += device
        val state = adapterState.value
        if (state != AdapterState.On) return Result.failure(BluetoothException.NotAvailable(state))
        return onConnect(device)
    }

    private fun BleDevice.matches(services: List<String>, namePrefix: String?): Boolean =
        (services.isEmpty() || advertisedServices.any { it in services }) &&
            (namePrefix == null || name?.startsWith(namePrefix, ignoreCase = true) == true)
}
