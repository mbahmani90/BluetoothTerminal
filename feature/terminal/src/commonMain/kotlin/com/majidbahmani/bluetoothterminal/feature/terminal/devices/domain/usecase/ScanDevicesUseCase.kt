package com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase

import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.BluetoothAvailability
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.DeviceScan
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.repository.DeviceRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Scanning rules for the Devices screen:
 * - scan only while Bluetooth is [BluetoothAvailability.Available]; otherwise say why not;
 * - start by itself once it becomes available (permission granted, Bluetooth switched on), stop when it doesn't;
 * - hide devices without a name unless [invoke]'s `showUnnamed` is true (most are beacons, not terminals).
 */
internal class ScanDevicesUseCase(private val repository: DeviceRepository) {

    @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
    operator fun invoke(showUnnamed: Boolean = false): Flow<DeviceScan> =
        repository.availability
            .distinctUntilChanged()
            .flatMapLatest { availability ->
                if (availability != BluetoothAvailability.Available) {
                    flowOf(DeviceScan.Unavailable(availability))
                } else {
                    repository.scan()
                        .map<_, DeviceScan> { devices -> DeviceScan.Scanning(devices.filter { showUnnamed || it.name != null }) }
                        .catch { emit(DeviceScan.Failed) }
                }
            }
}
