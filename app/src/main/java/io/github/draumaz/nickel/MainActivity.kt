package io.github.draumaz.nickel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.draumaz.nickel.ui.HomeScreen
import io.github.draumaz.nickel.ui.NickelViewModel
import io.github.draumaz.nickel.ui.SettingsScreen
import io.github.draumaz.nickel.ui.theme.NickelTheme

private enum class Screen { HOME, SETTINGS }

private const val BACK_MILLIS = 350

/** Finishes or cancels the back transition: quick, with no bounce. Animates its fraction. */
private val BackSettle = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: NickelViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.syncLedger() }
            NickelTheme(dynamicColor = settings.dynamicColor) {
                // Survives recreation; the seekable state below drives the animation.
                var saved by rememberSaveable { mutableStateOf(Screen.HOME) }
                val nav = remember { SeekableTransitionState(saved) }
                val transition = rememberTransition(nav, label = "screen")
                val scope = rememberCoroutineScope()
                val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
                val backFloat = tween<Float>(BACK_MILLIS, easing = LinearEasing)
                val backOffset = tween<IntOffset>(BACK_MILLIS, easing = LinearEasing)
                // Settings stays solid while it shrinks, so the two pages don't blur together,
                // and only fades out over the last stretch.
                val backFade = keyframes<Float> {
                    durationMillis = BACK_MILLIS
                    1f at BACK_MILLIS * 3 / 4 using LinearEasing
                }
                val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

                fun go(to: Screen) {
                    saved = to
                    scope.launch {
                        // Going back runs the linear back transition, so ease it with the spring.
                        if (to == Screen.HOME) nav.animateTo(to, BackSettle) else nav.animateTo(to)
                    }
                }

                // Keyed off `saved`, not the transition: scrubbing retargets the transition to Home,
                // which would otherwise switch the handler off mid-gesture.
                // Predictive back scrubs the real Settings -> Home transition, so Home slides in
                // behind the shrinking page as the finger moves, then finishes or springs back.
                PredictiveBackHandler(enabled = saved == Screen.SETTINGS) { events ->
                    try {
                        events.collect { nav.seekTo(it.progress, Screen.HOME) }
                        saved = Screen.HOME
                        // Settle with a quick spring from wherever the finger let go. Without a
                        // spec the rest of the linear transition plays at full length, which
                        // drags after a quick flick released early.
                        scope.launch { nav.animateTo(Screen.HOME, BackSettle) }
                    } catch (e: CancellationException) {
                        scope.launch { nav.animateTo(Screen.SETTINGS, BackSettle) }
                        throw e
                    }
                }

                transition.AnimatedContent(
                    transitionSpec = {
                        if (targetState == Screen.SETTINGS) {
                            (slideInHorizontally(spatial) { it / 3 } + fadeIn(effects))
                                .togetherWith(slideOutHorizontally(spatial) { -it / 8 } + fadeOut(effects))
                        } else {
                            // Settings shrinks away on top while Home comes in underneath. Linear
                            // tweens, because the back gesture seeks through this by progress and a
                            // spring would be almost finished a third of the way in.
                            (slideInHorizontally(backOffset) { -it / 8 } + fadeIn(backFloat))
                                .togetherWith(
                                    scaleOut(backFloat, targetScale = 0.86f) +
                                        slideOutHorizontally(backOffset) { it / 4 } + fadeOut(backFade)
                                )
                                .apply { targetContentZIndex = -1f }
                        }
                    },
                ) { current ->
                    // The page rounds its corners as it leaves, like a card being put away.
                    val corner by this.transition.animateDp(
                        transitionSpec = { tween(BACK_MILLIS / 4, easing = LinearEasing) },
                        label = "corner",
                    ) {
                        if (it == EnterExitState.PostExit) 40.dp else 0.dp
                    }
                    Box(Modifier.graphicsLayer {
                        shape = RoundedCornerShape(corner)
                        clip = corner > 0.dp
                    }) {
                        when (current) {
                            Screen.HOME -> HomeScreen(
                                viewModel = viewModel,
                                onOpenSettings = { go(Screen.SETTINGS) },
                            )
                            Screen.SETTINGS -> SettingsScreen(
                                viewModel = viewModel,
                                onBack = { go(Screen.HOME) },
                            )
                        }
                    }
                }
            }
        }
    }
}
