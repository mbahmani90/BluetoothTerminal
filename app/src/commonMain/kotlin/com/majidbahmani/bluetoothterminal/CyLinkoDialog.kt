package com.majidbahmani.bluetoothterminal

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import bluetoothterminal.app.generated.resources.Res
import bluetoothterminal.app.generated.resources.cylinko_dont_ask_again
import bluetoothterminal.app.generated.resources.cylinko_get
import bluetoothterminal.app.generated.resources.ic_cylinko_logo
import bluetoothterminal.app.generated.resources.cylinko_message
import bluetoothterminal.app.generated.resources.cylinko_not_now
import bluetoothterminal.app.generated.resources.cylinko_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** CyLinko's Google Play listing. */
private const val CYLINKO_PLAY_URL = "https://play.google.com/store/apps/details?id=com.cypressiot.cypressterminal"

/** Light blue fill for "Get CyLinko", with white text. */
private val GetButtonBlue = Color(0xFF2196F3)

/** Light gray for the quieter "Not now". */
private val NotNowGray = Color(0xFFB0B0B0)

/**
 * Suggests CyLinko, the professional BLE app, when the app opens. Both buttons close it, and so does tapping
 * outside; [onDismiss] gets whether "Don't ask again" was ticked at that moment.
 */
@Composable
internal fun CyLinkoDialog(onDismiss: (dontAskAgain: Boolean) -> Unit) {
    val uriHandler = LocalUriHandler.current
    var dontAskAgain by rememberSaveable { mutableStateOf(false) }
    val close = { onDismiss(dontAskAgain) }
    AlertDialog(
        onDismissRequest = close,
        icon = {
            Image(
                painter = painterResource(Res.drawable.ic_cylinko_logo),
                contentDescription = null, // the title names the app
                modifier = Modifier.size(64.dp),
            )
        },
        title = { Text(stringResource(Res.string.cylinko_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.cylinko_message))
                Row(
                    // The whole row toggles, not just the small box.
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = dontAskAgain, role = Role.Checkbox, onValueChange = { dontAskAgain = it }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = dontAskAgain,
                        onCheckedChange = null, // handled by the row
                        colors = CheckboxDefaults.colors(checkedColor = GetButtonBlue, checkmarkColor = Color.White),
                    )
                    Text(
                        stringResource(Res.string.cylinko_dont_ask_again),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    close()
                    // Opens the Play Store app if installed, else the browser.
                    runCatching { uriHandler.openUri(CYLINKO_PLAY_URL) }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GetButtonBlue, contentColor = Color.White),
            ) { Text(stringResource(Res.string.cylinko_get)) }
        },
        dismissButton = {
            TextButton(
                onClick = close,
                colors = ButtonDefaults.textButtonColors(contentColor = NotNowGray),
            ) { Text(stringResource(Res.string.cylinko_not_now), style = MaterialTheme.typography.labelMedium) }
        },
    )
}
