package com.majidbahmani.bluetoothterminal.feature.terminal.di

import com.majidbahmani.bluetoothterminal.feature.terminal.connection.di.connectionModule
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.di.devicesModule
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.di.settingsModule
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.di.terminalModule
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The feature's only public Koin module: `:app` loads this, never the package modules (doc 34).
 *
 * Needs from `:app`: `BluetoothClient` and `BluetoothSession`.
 */
val terminalFeatureModule: Module = module {
    includes(connectionModule, devicesModule, terminalModule, settingsModule)
}
