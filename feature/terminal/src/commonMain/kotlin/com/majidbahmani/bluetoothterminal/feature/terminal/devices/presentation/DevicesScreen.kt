package com.majidbahmani.bluetoothterminal.feature.terminal.devices.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bluetoothterminal.feature.terminal.generated.resources.Res
import bluetoothterminal.feature.terminal.generated.resources.connection_connected
import bluetoothterminal.feature.terminal.generated.resources.connection_connecting
import bluetoothterminal.feature.terminal.generated.resources.connection_failed_timeout
import bluetoothterminal.feature.terminal.generated.resources.connection_failed_unavailable
import bluetoothterminal.feature.terminal.generated.resources.connection_failed_unknown
import bluetoothterminal.feature.terminal.generated.resources.connection_lost
import bluetoothterminal.feature.terminal.generated.resources.devices_allow
import bluetoothterminal.feature.terminal.generated.resources.devices_bluetooth_off
import bluetoothterminal.feature.terminal.generated.resources.devices_checking
import bluetoothterminal.feature.terminal.generated.resources.devices_open_settings
import bluetoothterminal.feature.terminal.generated.resources.devices_permission_missing
import bluetoothterminal.feature.terminal.generated.resources.devices_retry
import bluetoothterminal.feature.terminal.generated.resources.devices_scan_failed
import bluetoothterminal.feature.terminal.generated.resources.devices_searching
import bluetoothterminal.feature.terminal.generated.resources.devices_signal
import bluetoothterminal.feature.terminal.generated.resources.devices_status_connected
import bluetoothterminal.feature.terminal.generated.resources.devices_status_connecting
import bluetoothterminal.feature.terminal.generated.resources.devices_title
import bluetoothterminal.feature.terminal.generated.resources.devices_turn_on
import bluetoothterminal.feature.terminal.generated.resources.devices_unnamed
import bluetoothterminal.feature.terminal.generated.resources.devices_unsupported
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceInfo
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.BluetoothAvailability
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.Device
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.domain.model.DeviceScan
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DevicesRoute(
    modifier: Modifier = Modifier,
    viewModel: DevicesViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bluetoothActions = rememberBluetoothActions()

    DevicesScreen(
        uiState = uiState,
        onDeviceClick = viewModel::onDeviceClick,
        onRetry = viewModel::onRetry,
        onRequestPermission = bluetoothActions.requestPermission,
        onEnableBluetooth = bluetoothActions.enableBluetooth,
        onOpenSettings = bluetoothActions.openSettings,
        modifier = modifier,
    )
}

@Composable
internal fun DevicesScreen(
    uiState: DevicesUiState,
    onDeviceClick: (Device) -> Unit,
    onRetry: () -> Unit,
    onRequestPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(Res.string.devices_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        ConnectionBanner(uiState.connection)

        when (val scan = uiState.scan) {
            is DeviceScan.Unavailable -> UnavailableContent(
                availability = scan.availability,
                onRequestPermission = onRequestPermission,
                onEnableBluetooth = onEnableBluetooth,
                onOpenSettings = onOpenSettings,
            )
            is DeviceScan.Scanning -> DeviceList(
                devices = scan.devices,
                isScanning = !uiState.isScanPaused,
                connection = uiState.connection,
                onDeviceClick = onDeviceClick,
            )
            DeviceScan.Failed -> Message(
                text = stringResource(Res.string.devices_scan_failed),
                action = stringResource(Res.string.devices_retry) to onRetry,
            )
        }
    }
}

@Composable
private fun ConnectionBanner(connection: ConnectionStatus) {
    val text = when (connection) {
        ConnectionStatus.Disconnected -> return
        is ConnectionStatus.Connecting -> stringResource(Res.string.connection_connecting, connection.device.displayName())
        is ConnectionStatus.Connected -> stringResource(Res.string.connection_connected, connection.device.displayName())
        is ConnectionStatus.Failed -> stringResource(connection.error.message(), connection.device.displayName())
        is ConnectionStatus.Lost -> stringResource(Res.string.connection_lost, connection.device.displayName())
    }
    val isProblem = connection is ConnectionStatus.Failed || connection is ConnectionStatus.Lost
    Surface(
        color = if (isProblem) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(text = text, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
    }
}

@Composable
private fun UnavailableContent(
    availability: BluetoothAvailability,
    onRequestPermission: () -> Unit,
    onEnableBluetooth: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    when (availability) {
        BluetoothAvailability.Checking -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        BluetoothAvailability.Unsupported -> Message(text = stringResource(Res.string.devices_unsupported))
        BluetoothAvailability.PermissionMissing -> Message(
            text = stringResource(Res.string.devices_permission_missing),
            action = stringResource(Res.string.devices_allow) to onRequestPermission,
            secondaryAction = stringResource(Res.string.devices_open_settings) to onOpenSettings,
        )
        BluetoothAvailability.Off -> Message(
            text = stringResource(Res.string.devices_bluetooth_off),
            action = stringResource(Res.string.devices_turn_on) to onEnableBluetooth,
        )
        // Not shown as Unavailable; listed so the when stays exhaustive.
        BluetoothAvailability.Available -> Unit
    }
}

@Composable
private fun DeviceList(
    devices: List<Device>,
    isScanning: Boolean,
    connection: ConnectionStatus,
    onDeviceClick: (Device) -> Unit,
) {
    if (isScanning) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    if (devices.isEmpty()) {
        Message(text = stringResource(Res.string.devices_searching))
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(items = devices, key = { it.id }) { device ->
            ListItem(
                headlineContent = { Text(device.name ?: stringResource(Res.string.devices_unnamed)) },
                supportingContent = { Text(device.id) },
                trailingContent = { Text(device.trailingText(connection)) },
                modifier = Modifier.clickable { onDeviceClick(device) },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun Message(
    text: String,
    action: Pair<String, () -> Unit>? = null,
    secondaryAction: Pair<String, () -> Unit>? = null,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = text, textAlign = TextAlign.Center)
        action?.let { (label, onClick) -> Button(onClick = onClick) { Text(label) } }
        secondaryAction?.let { (label, onClick) -> TextButton(onClick = onClick) { Text(label) } }
    }
}

/** "Connecting…" / "Connected" for the active device, the signal strength for the others. */
@Composable
private fun Device.trailingText(connection: ConnectionStatus): String = when {
    connection is ConnectionStatus.Connecting && connection.device.id == id ->
        stringResource(Res.string.devices_status_connecting)
    connection is ConnectionStatus.Connected && connection.device.id == id ->
        stringResource(Res.string.devices_status_connected)
    else -> stringResource(Res.string.devices_signal, signalStrength)
}

@Composable
private fun DeviceInfo.displayName(): String = name ?: stringResource(Res.string.devices_unnamed)

private fun ConnectionError.message(): StringResource = when (this) {
    ConnectionError.BluetoothUnavailable -> Res.string.connection_failed_unavailable
    ConnectionError.Timeout -> Res.string.connection_failed_timeout
    ConnectionError.Unknown -> Res.string.connection_failed_unknown
}

@Preview
@Composable
private fun DevicesScreenScanningPreview() {
    MaterialTheme {
        DevicesScreen(
            uiState = DevicesUiState(
                scan = DeviceScan.Scanning(
                    listOf(
                        Device(id = "AA:BB:CC:DD:EE:01", name = "HM-10", signalStrength = -58),
                        Device(id = "AA:BB:CC:DD:EE:02", name = "OBDII", signalStrength = -71),
                    ),
                ),
                connection = ConnectionStatus.Connecting(DeviceInfo(id = "AA:BB:CC:DD:EE:01", name = "HM-10")),
            ),
            onDeviceClick = {},
            onRetry = {},
            onRequestPermission = {},
            onEnableBluetooth = {},
            onOpenSettings = {},
        )
    }
}

@Preview
@Composable
private fun DevicesScreenPermissionPreview() {
    MaterialTheme {
        DevicesScreen(
            uiState = DevicesUiState(scan = DeviceScan.Unavailable(BluetoothAvailability.PermissionMissing)),
            onDeviceClick = {},
            onRetry = {},
            onRequestPermission = {},
            onEnableBluetooth = {},
            onOpenSettings = {},
        )
    }
}
