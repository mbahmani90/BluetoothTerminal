package com.majidbahmani.bluetoothterminal.di

import com.majidbahmani.bluetooth.BluetoothSession
import com.majidbahmani.bluetoothterminal.feature.terminal.di.terminalFeatureModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Provides the platform's `BluetoothClient` (Android needs a `Context`, iOS doesn't). */
internal expect val platformModule: Module

internal const val APP_SCOPE = "appScope"

/** One Bluetooth client, one app-lifetime scope and one session for the whole app (doc 14, doc 49). */
internal val bluetoothModule = module {
    single<CoroutineScope>(named(APP_SCOPE)) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    single { BluetoothSession(client = get(), scope = get(named(APP_SCOPE))) }
}

internal val appModules: List<Module> = listOf(platformModule, bluetoothModule, terminalFeatureModule)

/** Starts Koin once per process: from the Android `Application`, and from Swift on iOS. */
fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(appModules)
}
