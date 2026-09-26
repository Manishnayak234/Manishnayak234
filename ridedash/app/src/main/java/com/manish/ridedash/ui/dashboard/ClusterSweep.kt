package com.manish.ridedash.ui.dashboard

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * The key-on sweep: the gauge runs up the whole scale and back before it starts showing real speed,
 * the way the bike's own cluster does when the ignition comes on.
 *
 * It is the one bit of theatre in the app, and it earns its place twice over: it is also a self-test.
 * A rider sees the entire arc and every scale label before setting off, so a dead pixel or a wrong
 * font shows up in the garage rather than at 80 km/h.
 */
class ClusterSweep internal constructor(
    private val scope: CoroutineScope,
    private val maxKmh: Float,
) {
    /** The speed to display while the sweep runs, or null once live speed should take over. */
    var value: Float? by mutableStateOf(null)
        private set

    private var job: Job? = null

    val running: Boolean get() = value != null

    internal fun start() {
        job?.cancel()
        job = scope.launch {
            try {
                animate(
                    initialValue = 0f,
                    targetValue = maxKmh,
                    animationSpec = tween(RISE_MS, easing = FastOutSlowInEasing),
                ) { swept, _ -> value = swept }
                animate(
                    initialValue = maxKmh,
                    targetValue = 0f,
                    animationSpec = tween(FALL_MS, easing = FastOutSlowInEasing),
                ) { swept, _ -> value = swept }
            } finally {
                // Finished, skipped or left the screen: all three hand over to live speed.
                value = null
            }
        }
    }

    /** A tap anywhere on the dashboard cuts the theatre short. */
    fun skip() {
        job?.cancel()
    }
}

/**
 * [token] is bumped each time dashboard mode is entered, so the sweep runs once per key-on rather
 * than on every recomposition or every trip to the stats screen.
 */
@Composable
fun rememberClusterSweep(token: Int, maxKmh: Float = MAX_SCALE_KMH): ClusterSweep {
    val scope = rememberCoroutineScope()
    val sweep = remember(maxKmh) { ClusterSweep(scope, maxKmh) }
    LaunchedEffect(token) { sweep.start() }
    return sweep
}

/** Up in just over three quarters of a second, back down a shade quicker: about 1.4 s in all. */
private const val RISE_MS = 800
private const val FALL_MS = 600
