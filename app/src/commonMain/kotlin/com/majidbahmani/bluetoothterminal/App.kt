package com.majidbahmani.bluetoothterminal

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.majidbahmani.bluetoothterminal.feature.terminal.navigation.TerminalGraph
import com.majidbahmani.bluetoothterminal.feature.terminal.navigation.terminalScreens

/** Composition root UI: theme + navigation. Koin must be started before (Application / iOS entry point). */
@Composable
fun App() {
    // Material defaults until :systemdesign has the app theme (doc 36).
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = TerminalGraph,
                modifier = Modifier.safeDrawingPadding(),
            ) {
                terminalScreens(navController)
            }
        }
    }
}
