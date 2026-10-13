package com.majidbahmani.bluetoothterminal.di

import com.majidbahmani.bluetooth.BluetoothClient
import com.majidbahmani.bluetooth.createBluetoothClient
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformModule: Module = module {
    single<BluetoothClient> { createBluetoothClient() }
}
