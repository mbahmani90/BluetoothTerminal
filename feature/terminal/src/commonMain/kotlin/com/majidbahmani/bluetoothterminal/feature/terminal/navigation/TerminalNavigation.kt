package com.majidbahmani.bluetoothterminal.feature.terminal.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.presentation.DevicesRoute
import com.majidbahmani.bluetoothterminal.feature.terminal.settings.presentation.SettingsRoute
import com.majidbahmani.bluetoothterminal.feature.terminal.terminal.presentation.TerminalRoute
import kotlinx.serialization.Serializable

/** The feature's graph; `:app` navigates to this, never to a screen inside it (doc 34). */
@Serializable
data object TerminalGraph

@Serializable
internal data object DevicesDestination

@Serializable
internal data object TerminalDestination

@Serializable
internal data object SettingsDestination

/**
 * The feature's one entry point: all its screens, Devices first.
 *
 * Devices opens the Terminal once connected (or when the connected device is tapped). Back, or Devices in the
 * Terminal's menu, returns to Devices and keeps the connection; Disconnect ends it and returns to Devices.
 * Settings (Terminal's menu) lists the device's services; Back returns to the Terminal.
 */
fun NavGraphBuilder.terminalScreens(navController: NavController) {
    val backToDevices = { navController.popBackStack(DevicesDestination, inclusive = false) }
    navigation<TerminalGraph>(startDestination = DevicesDestination) {
        composable<DevicesDestination> {
            DevicesRoute(onOpenTerminal = { navController.navigate(TerminalDestination) { launchSingleTop = true } })
        }
        composable<TerminalDestination> {
            TerminalRoute(
                onOpenDevices = { backToDevices() },
                onOpenSettings = { navController.navigate(SettingsDestination) { launchSingleTop = true } },
                onClose = { backToDevices() },
            )
        }
        composable<SettingsDestination> {
            // Pops only Settings: a double tap can't also close the Terminal.
            SettingsRoute(onBack = { navController.popBackStack(SettingsDestination, inclusive = true) })
        }
    }
}
