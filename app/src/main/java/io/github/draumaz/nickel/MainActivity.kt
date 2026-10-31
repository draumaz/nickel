package io.github.draumaz.nickel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.IntOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.draumaz.nickel.ui.HomeScreen
import io.github.draumaz.nickel.ui.NickelViewModel
import io.github.draumaz.nickel.ui.SettingsScreen
import io.github.draumaz.nickel.ui.theme.NickelTheme

private enum class Screen { HOME, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: NickelViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            NickelTheme(dynamicColor = settings.dynamicColor) {
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = {
                        if (targetState == Screen.SETTINGS) {
                            (slideInHorizontally(spatial) { it / 3 } + fadeIn(effects))
                                .togetherWith(slideOutHorizontally(spatial) { -it / 8 } + fadeOut(effects))
                        } else {
                            (slideInHorizontally(spatial) { -it / 8 } + fadeIn(effects))
                                .togetherWith(slideOutHorizontally(spatial) { it / 3 } + fadeOut(effects))
                        }
                    },
                    label = "screen",
                ) { current ->
                    when (current) {
                        Screen.HOME -> HomeScreen(
                            viewModel = viewModel,
                            onOpenSettings = { screen = Screen.SETTINGS },
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            onBack = { screen = Screen.HOME },
                        )
                    }
                }
            }
        }
    }
}
