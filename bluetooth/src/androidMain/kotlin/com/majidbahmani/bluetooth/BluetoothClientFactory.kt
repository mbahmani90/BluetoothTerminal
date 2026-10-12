package com.majidbahmani.bluetooth

import android.content.Context

/**
 * Creates the Android [BluetoothClient].
 *
 * Create it once (e.g. a DI singleton): it registers listeners for the Bluetooth state for the app's lifetime.
 */
public fun createBluetoothClient(context: Context): BluetoothClient = AndroidBleClient(context.applicationContext)
