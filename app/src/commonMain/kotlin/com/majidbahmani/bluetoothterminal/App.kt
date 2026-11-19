package com.majidbahmani.bluetoothterminal

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.majidbahmani.bluetoothterminal.feature.terminal.navigation.TerminalGraph
import com.majidbahmani.bluetoothterminal.feature.terminal.navigation.terminalScreens
import org.koin.compose.koinInject

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
                // Standard push instead of the default 700 ms crossfade: the new screen slides in from the
                // end, Back slides it out again.
                enterTransition = { slideIntoContainer(SlideDirection.Start, tween(TRANSITION_MILLIS)) },
                exitTransition = { slideOutOfContainer(SlideDirection.Start, tween(TRANSITION_MILLIS)) },
                popEnterTransition = { slideIntoContainer(SlideDirection.End, tween(TRANSITION_MILLIS)) },
                popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(TRANSITION_MILLIS)) },
            ) {
                terminalScreens(navController)
            }

            // Shown once each time the app opens, unless "Don't ask again" was ticked before; saveable so
            // rotating the phone doesn't bring it back.
            val cyLinkoPrompt = koinInject<CyLinkoPromptStore>()
            var showCyLinko by rememberSaveable { mutableStateOf(!cyLinkoPrompt.isMuted) }
            if (showCyLinko) {
                CyLinkoDialog(onDismiss = { dontAskAgain ->
                    if (dontAskAgain) cyLinkoPrompt.mute()
                    showCyLinko = false
                })
            }
        }
    }
}

private const val TRANSITION_MILLIS = 300
