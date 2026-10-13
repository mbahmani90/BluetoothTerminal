package com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.BluetoothAvailability
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.Device
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.DeviceScan
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.repository.DeviceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

/**
 * Scanning rules for the Devices screen:
 * - scan only while Bluetooth is [BluetoothAvailability.Available]; otherwise say why not;
 * - start by itself once it becomes available (permission granted, Bluetooth switched on), stop when it doesn't;
 * - stop after [SCAN_DURATION] (battery), or when [invoke]'s `stop` emits, and keep the devices found as
 *   [DeviceScan.Finished];
 * - hide devices without a name unless [invoke]'s `showUnnamed` is true (most are beacons, not terminals).
 */
internal class ScanDevicesUseCase(private val repository: DeviceRepository) {

    @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
    operator fun invoke(showUnnamed: Boolean = false, stop: Flow<Unit> = emptyFlow()): Flow<DeviceScan> =
        repository.availability
            .distinctUntilChanged()
            .flatMapLatest { availability ->
                if (availability != BluetoothAvailability.Available) {
                    flowOf(DeviceScan.Unavailable(availability))
                } else {
                    timedScan(showUnnamed, stop).catch { emit(DeviceScan.Failed) }
                }
            }

    private fun timedScan(showUnnamed: Boolean, stop: Flow<Unit>): Flow<DeviceScan> = flow {
        var devices = emptyList<Device>()
        withTimeoutOrNull(SCAN_DURATION) {
            // A stop request arrives as null and ends the collection, which cancels the scan.
            merge(repository.scan(), stop.map { null }).takeWhile { it != null }.collect { found ->
                devices = found.orEmpty().filter { showUnnamed || it.name != null }
                emit(DeviceScan.Scanning(devices))
            }
        }
        emit(DeviceScan.Finished(devices))
    }

    internal companion object {
        val SCAN_DURATION = 30.seconds
    }
}
