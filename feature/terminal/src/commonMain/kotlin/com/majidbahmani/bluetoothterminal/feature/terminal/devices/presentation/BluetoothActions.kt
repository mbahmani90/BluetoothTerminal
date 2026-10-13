package com.majidbahmani.bluetoothterminal.feature.terminal.devices.presentation

import androidx.compose.runtime.Composable

/** What the user can do when Bluetooth can't be used. Each platform decides how (permission dialog, settings, …). */
internal class BluetoothActions(
    val requestPermission: () -> Unit,
    val enableBluetooth: () -> Unit,
    val openSettings: () -> Unit,
)

@Composable
internal expect fun rememberBluetoothActions(): BluetoothActions
