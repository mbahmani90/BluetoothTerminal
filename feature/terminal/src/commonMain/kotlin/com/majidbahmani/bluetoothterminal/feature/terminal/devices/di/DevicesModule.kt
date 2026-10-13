package com.majidbahmani.bluetoothterminal.feature.terminal.devices.di

import com.majidbahmani.bluetoothterminal.feature.terminal.devices.data.repository.DeviceRepositoryImpl
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.repository.DeviceRepository
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase.ConnectToDeviceUseCase
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.usecase.ScanDevicesUseCase
import org.koin.dsl.module

/** Needs `BluetoothClient` and `BluetoothSession` (from `:app`) and `ConnectionRepository` (connection package). */
internal val devicesModule = module {
    single<DeviceRepository> { DeviceRepositoryImpl(client = get(), session = get()) }
    factory { ScanDevicesUseCase(repository = get()) }
    factory { ConnectToDeviceUseCase(devices = get(), connection = get()) }
}
