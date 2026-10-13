package com.majidbahmani.bluetoothterminal

import android.app.Application
import com.majidbahmani.bluetoothterminal.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

/** Starts Koin once per process (registered in the manifest; without it: "KoinApplication has not been started"). */
class BluetoothTerminalApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@BluetoothTerminalApp)
        }
    }
}
