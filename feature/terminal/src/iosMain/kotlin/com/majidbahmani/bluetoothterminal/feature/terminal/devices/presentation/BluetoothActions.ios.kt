package com.majidbahmani.bluetoothterminal.feature.terminal.devices.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

/**
 * iOS shows the Bluetooth permission prompt by itself (the first time CoreBluetooth is used) and has no
 * API to switch Bluetooth on: once denied or off, the user fixes it in Settings.
 */
@Composable
internal actual fun rememberBluetoothActions(): BluetoothActions = remember {
    val openSettings = {
        NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        }
        Unit
    }
    BluetoothActions(requestPermission = openSettings, enableBluetooth = openSettings, openSettings = openSettings)
}
