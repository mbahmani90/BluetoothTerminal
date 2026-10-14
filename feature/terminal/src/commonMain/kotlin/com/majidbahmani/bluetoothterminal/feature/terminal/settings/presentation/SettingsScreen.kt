package com.majidbahmani.bluetoothterminal.feature.terminal.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import bluetoothterminal.feature.terminal.generated.resources.Res
import bluetoothterminal.feature.terminal.generated.resources.ic_arrow_back
import bluetoothterminal.feature.terminal.generated.resources.settings_back
import bluetoothterminal.feature.terminal.generated.resources.settings_listen
import bluetoothterminal.feature.terminal.generated.resources.settings_not_connected
import bluetoothterminal.feature.terminal.generated.resources.settings_property_notify
import bluetoothterminal.feature.terminal.generated.resources.settings_property_read
import bluetoothterminal.feature.terminal.generated.resources.settings_property_write
import bluetoothterminal.feature.terminal.generated.resources.settings_read
import bluetoothterminal.feature.terminal.generated.resources.settings_read_empty
import bluetoothterminal.feature.terminal.generated.resources.settings_read_failed
import bluetoothterminal.feature.terminal.generated.resources.settings_reading
import bluetoothterminal.feature.terminal.generated.resources.settings_send_here
import bluetoothterminal.feature.terminal.generated.resources.settings_title
import bluetoothterminal.feature.terminal.generated.resources.settings_unknown_service
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.CharacteristicRef
import com.majidbahmani.bluetoothterminal.feature.terminal.connection.domain.model.DeviceCharacteristic
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.model.CharacteristicSettings
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.domain.model.ServiceSettings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        uiState = uiState,
        onSendTargetSelect = viewModel::onSendTargetSelect,
        onListeningChange = viewModel::onListeningChange,
        onRead = viewModel::onRead,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    onSendTargetSelect: (CharacteristicRef) -> Unit,
    onListeningChange: (CharacteristicRef, Boolean) -> Unit,
    onRead: (CharacteristicRef) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = stringResource(Res.string.settings_back))
            }
            Text(
                text = stringResource(Res.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        if (uiState.services.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.settings_not_connected),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items = uiState.services, key = { it.uuid }) { service ->
                ServiceCard(
                    service = service,
                    reads = uiState.reads,
                    onSendTargetSelect = onSendTargetSelect,
                    onListeningChange = onListeningChange,
                    onRead = onRead,
                )
            }
        }
    }
}

/** The service's name (or "Unknown service") with its UUID below, then its characteristics. */
@Composable
private fun ServiceCard(
    service: ServiceSettings,
    reads: Map<CharacteristicRef, ReadResult>,
    onSendTargetSelect: (CharacteristicRef) -> Unit,
    onListeningChange: (CharacteristicRef, Boolean) -> Unit,
    onRead: (CharacteristicRef) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = KNOWN_NAMES[service.uuid] ?: stringResource(Res.string.settings_unknown_service),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Uuid(service.uuid, modifier = Modifier.padding(horizontal = 16.dp))
            service.characteristics.forEach { item ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                CharacteristicRow(
                    item = item,
                    read = reads[item.characteristic.ref],
                    onSendTargetSelect = onSendTargetSelect,
                    onListeningChange = onListeningChange,
                    onRead = onRead,
                )
            }
        }
    }
}

/** UUID, what it supports, then only the controls that apply: Send here, Listen, Read. */
@Composable
private fun CharacteristicRow(
    item: CharacteristicSettings,
    read: ReadResult?,
    onSendTargetSelect: (CharacteristicRef) -> Unit,
    onListeningChange: (CharacteristicRef, Boolean) -> Unit,
    onRead: (CharacteristicRef) -> Unit,
) {
    val characteristic = item.characteristic
    val ref = characteristic.ref
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        KNOWN_NAMES[ref.characteristic]?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
        Uuid(ref.characteristic)
        Text(
            text = characteristic.propertiesText(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (characteristic.canWrite) {
                // Radio: only one characteristic in the whole list is the send target.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.selectable(
                        selected = item.isSendTarget,
                        onClick = { onSendTargetSelect(ref) },
                        role = Role.RadioButton,
                    ),
                ) {
                    RadioButton(selected = item.isSendTarget, onClick = null)
                    Text(stringResource(Res.string.settings_send_here))
                }
            }
            if (characteristic.canNotify) {
                // Checkbox: several can be listened to at once.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.toggleable(
                        value = item.isListening,
                        onValueChange = { onListeningChange(ref, it) },
                        role = Role.Checkbox,
                    ),
                ) {
                    Checkbox(checked = item.isListening, onCheckedChange = null)
                    Text(stringResource(Res.string.settings_listen))
                }
            }
            if (characteristic.canRead) {
                TextButton(onClick = { onRead(ref) }, enabled = read != ReadResult.Reading) {
                    Text(stringResource(Res.string.settings_read))
                }
            }
        }
        read?.let { ReadValue(it) }
    }
}

@Composable
private fun ReadValue(read: ReadResult) {
    val (text, isError) = when (read) {
        ReadResult.Reading -> stringResource(Res.string.settings_reading) to false
        is ReadResult.Value -> read.value.ifEmpty { stringResource(Res.string.settings_read_empty) } to false
        is ReadResult.Failed -> stringResource(Res.string.settings_read_failed) to true
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        fontFamily = FontFamily.Monospace,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
    )
}

/** Light gray, like the characteristic labels in the Terminal log. */
@Composable
private fun Uuid(uuid: String, modifier: Modifier = Modifier) {
    Text(
        text = uuid,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun DeviceCharacteristic.propertiesText(): String = listOfNotNull(
    stringResource(Res.string.settings_property_read).takeIf { canRead },
    stringResource(Res.string.settings_property_write).takeIf { canWrite },
    stringResource(Res.string.settings_property_notify).takeIf { canNotify },
).joinToString(" · ")

/** Names for common services and characteristics; anything else shows its UUID only. */
private val KNOWN_NAMES: Map<String, String> = mapOf(
    "00001800-0000-1000-8000-00805f9b34fb" to "Generic Access",
    "00001801-0000-1000-8000-00805f9b34fb" to "Generic Attribute",
    "0000180a-0000-1000-8000-00805f9b34fb" to "Device Information",
    "0000180f-0000-1000-8000-00805f9b34fb" to "Battery",
    "00002a00-0000-1000-8000-00805f9b34fb" to "Device Name",
    "00002a01-0000-1000-8000-00805f9b34fb" to "Appearance",
    "00002a05-0000-1000-8000-00805f9b34fb" to "Service Changed",
    "00002a19-0000-1000-8000-00805f9b34fb" to "Battery Level",
    "6e400001-b5a3-f393-e0a9-e50e24dcca9e" to "Nordic UART",
    "6e400002-b5a3-f393-e0a9-e50e24dcca9e" to "UART RX (write)",
    "6e400003-b5a3-f393-e0a9-e50e24dcca9e" to "UART TX (notify)",
    "0000ffe0-0000-1000-8000-00805f9b34fb" to "HM-10 Serial",
)

@Preview
@Composable
private fun SettingsScreenPreview() {
    val nus = "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
    val gap = "00001800-0000-1000-8000-00805f9b34fb"
    val rx = DeviceCharacteristic(CharacteristicRef(nus, "6e400002-b5a3-f393-e0a9-e50e24dcca9e"), canRead = false, canWrite = true, canNotify = false)
    val tx = DeviceCharacteristic(CharacteristicRef(nus, "6e400003-b5a3-f393-e0a9-e50e24dcca9e"), canRead = false, canWrite = false, canNotify = true)
    val name = DeviceCharacteristic(CharacteristicRef(gap, "00002a00-0000-1000-8000-00805f9b34fb"), canRead = true, canWrite = false, canNotify = false)
    MaterialTheme {
        SettingsScreen(
            uiState = SettingsUiState(
                services = listOf(
                    ServiceSettings(gap, listOf(CharacteristicSettings(name, isSendTarget = false, isListening = false))),
                    ServiceSettings(
                        nus,
                        listOf(
                            CharacteristicSettings(rx, isSendTarget = true, isListening = false),
                            CharacteristicSettings(tx, isSendTarget = false, isListening = true),
                        ),
                    ),
                ),
                reads = mapOf(name.ref to ReadResult.Value("ESP32-NUS")),
            ),
            onSendTargetSelect = {},
            onListeningChange = { _, _ -> },
            onRead = {},
            onBack = {},
        )
    }
}
