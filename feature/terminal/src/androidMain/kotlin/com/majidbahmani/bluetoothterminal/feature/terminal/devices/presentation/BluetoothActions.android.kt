package com.majidbahmani.bluetoothterminal.feature.terminal.devices.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * No result callbacks needed: the library re-checks the adapter state when the activity resumes,
 * i.e. right after the permission dialog, the "turn on Bluetooth" dialog or Settings.
 */
@Composable
internal actual fun rememberBluetoothActions(): BluetoothActions {
    val context = LocalContext.current
    return remember(context) {
        BluetoothActions(
            requestPermission = {
                context.findActivity()?.requestPermissions(
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                    PERMISSION_REQUEST_CODE,
                )
            },
            enableBluetooth = { context.startSafely(enableBluetoothIntent()) },
            openSettings = {
                context.startSafely(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                )
            },
        )
    }
}

// Only offered when the permission is granted (AdapterState checks it before Off).
@SuppressLint("MissingPermission")
private fun enableBluetoothIntent() = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

private fun Context.startSafely(intent: Intent) {
    if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { startActivity(intent) } // e.g. no Settings app on a test device
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val PERMISSION_REQUEST_CODE = 1
