package com.majidbahmani.bluetoothterminal.feature.terminal.terminal.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bluetoothterminal.feature.terminal.generated.resources.Res
import bluetoothterminal.feature.terminal.generated.resources.connection_lost
import bluetoothterminal.feature.terminal.generated.resources.devices_unnamed
import bluetoothterminal.feature.terminal.generated.resources.ic_more_vert
import bluetoothterminal.feature.terminal.generated.resources.terminal_disconnect
import bluetoothterminal.feature.terminal.generated.resources.terminal_empty
import bluetoothterminal.feature.terminal.generated.resources.terminal_error_failed
import bluetoothterminal.feature.terminal.generated.resources.terminal_error_no_target
import bluetoothterminal.feature.terminal.generated.resources.terminal_error_not_connected
import bluetoothterminal.feature.terminal.generated.resources.terminal_error_not_supported
import bluetoothterminal.feature.terminal.generated.resources.terminal_input_hint
import bluetoothterminal.feature.terminal.generated.resources.terminal_menu_devices
import bluetoothterminal.feature.terminal.generated.resources.terminal_menu_settings
import bluetoothterminal.feature.terminal.generated.resources.terminal_more
import bluetoothterminal.feature.terminal.generated.resources.terminal_send
import bluetoothterminal.feature.terminal.generated.resources.terminal_title
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ChannelError
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.ConnectionStatus
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceInfo
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.domain.model.TerminalMessage
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun TerminalRoute(
    onOpenDevices: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TerminalViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnClose by rememberUpdatedState(onClose)
    LaunchedEffect(viewModel) { viewModel.closed.collect { currentOnClose() } }

    TerminalScreen(
        uiState = uiState,
        onInputChange = viewModel::onInputChange,
        onSend = viewModel::onSend,
        onDisconnect = viewModel::onDisconnect,
        onOpenDevices = onOpenDevices,
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
}

@Composable
internal fun TerminalScreen(
    uiState: TerminalUiState,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onDisconnect: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The keyboard is handled once, by App's safeDrawingPadding (it includes the IME insets).
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Text(
                text = uiState.connection.deviceName() ?: stringResource(Res.string.terminal_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            if (uiState.isConnected) {
                TextButton(onClick = onDisconnect) { Text(stringResource(Res.string.terminal_disconnect)) }
            }
            MoreMenu(onOpenDevices = onOpenDevices, onOpenSettings = onOpenSettings)
        }
        val lost = uiState.connection as? ConnectionStatus.Lost
        if (lost != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.connection_lost, lost.device.name ?: stringResource(Res.string.devices_unnamed)),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        MessageLog(messages = uiState.messages, modifier = Modifier.weight(1f))

        uiState.sendError?.let { error ->
            Text(
                text = stringResource(error.message()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        InputRow(
            input = uiState.input,
            canSend = uiState.canSend,
            enabled = uiState.isConnected,
            onInputChange = onInputChange,
            onSend = onSend,
        )
    }
}

/** ⋮ in the top bar: Devices goes back to the device list (the connection stays); Settings lists the services. */
@Composable
private fun MoreMenu(onOpenDevices: () -> Unit, onOpenSettings: () -> Unit) {
    var expanded by remember { mutableStateOf(false) } // UI-only: whether the menu is open
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(Res.drawable.ic_more_vert), contentDescription = stringResource(Res.string.terminal_more))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.terminal_menu_devices)) },
                onClick = {
                    expanded = false
                    onOpenDevices()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.terminal_menu_settings)) },
                onClick = {
                    expanded = false
                    onOpenSettings()
                },
            )
        }
    }
}

@Composable
private fun MessageLog(messages: List<TerminalMessage>, modifier: Modifier) {
    if (messages.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(Res.string.terminal_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val listState = rememberLazyListState()
    // Follow new messages.
    LaunchedEffect(messages.size) { listState.animateScrollToItem(messages.lastIndex) }
    LazyColumn(state = listState, modifier = modifier.fillMaxWidth()) {
        items(messages) { message -> MessageItem(message) }
    }
}

/** The characteristic in light gray, the text below it in the main text color; "→" marks what was sent. */
@Composable
private fun MessageItem(message: TerminalMessage) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        val arrow = if (message.direction == TerminalMessage.Direction.Sent) "→ " else ""
        Text(
            text = arrow + message.characteristic.characteristic,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun InputRow(
    input: String,
    canSend: Boolean,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            enabled = enabled,
            singleLine = true,
            placeholder = { Text(stringResource(Res.string.terminal_input_hint)) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            modifier = Modifier.weight(1f),
        )
        Button(onClick = onSend, enabled = canSend) { Text(stringResource(Res.string.terminal_send)) }
    }
}

private fun ConnectionStatus.deviceName(): String? = when (this) {
    ConnectionStatus.Disconnected -> null
    is ConnectionStatus.Connecting -> device.name
    is ConnectionStatus.Connected -> device.name
    is ConnectionStatus.Failed -> device.name
    is ConnectionStatus.Lost -> device.name
}

private fun ChannelError.message(): StringResource = when (this) {
    ChannelError.NotConnected -> Res.string.terminal_error_not_connected
    ChannelError.NoSendTarget -> Res.string.terminal_error_no_target
    ChannelError.NotSupported -> Res.string.terminal_error_not_supported
    ChannelError.Failed -> Res.string.terminal_error_failed
}

@Preview
@Composable
private fun TerminalScreenPreview() {
    val rx = CharacteristicRef("6e400001-b5a3-f393-e0a9-e50e24dcca9e", "6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    val tx = CharacteristicRef("6e400001-b5a3-f393-e0a9-e50e24dcca9e", "6e400003-b5a3-f393-e0a9-e50e24dcca9e")
    MaterialTheme {
        TerminalScreen(
            uiState = TerminalUiState(
                connection = ConnectionStatus.Connected(DeviceInfo(id = "1", name = "ESP32-NUS")),
                messages = listOf(
                    TerminalMessage(TerminalMessage.Direction.Received, tx, "tick 1"),
                    TerminalMessage(TerminalMessage.Direction.Sent, rx, "hello"),
                    TerminalMessage(TerminalMessage.Direction.Received, tx, "hello"),
                ),
                input = "status",
            ),
            onInputChange = {},
            onSend = {},
            onDisconnect = {},
            onOpenDevices = {},
            onOpenSettings = {},
        )
    }
}
