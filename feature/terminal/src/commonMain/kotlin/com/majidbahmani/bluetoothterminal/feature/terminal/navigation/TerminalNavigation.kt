package com.majidbahmani.bluetoothterminal.feature.terminal.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.majidbahmani.bluetoothterminal.feature.terminal.devices.presentation.DevicesRoute
import kotlinx.serialization.Serializable

/** The feature's graph; `:app` navigates to this, never to a screen inside it (doc 34). */
@Serializable
data object TerminalGraph

@Serializable
internal data object DevicesDestination

/** The feature's one entry point: all its screens, Devices first. */
fun NavGraphBuilder.terminalScreens() {
    navigation<TerminalGraph>(startDestination = DevicesDestination) {
        composable<DevicesDestination> { DevicesRoute() }
    }
}
