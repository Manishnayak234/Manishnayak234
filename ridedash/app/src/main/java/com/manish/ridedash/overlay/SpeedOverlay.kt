package com.manish.ridedash.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manish.ridedash.data.RideRepository
import com.manish.ridedash.data.RideState
import com.manish.ridedash.ui.theme.RideDashTheme
import com.manish.ridedash.ui.theme.numberStyle
import com.manish.ridedash.ui.theme.rideColors
import com.manish.ridedash.util.Formatters
import kotlin.math.roundToInt

/**
 * The floating speed box that sits over the Google Maps app (screen 4.3).
 *
 * Maps keeps its own full-screen map, route and position; all we add is the speed, because that is
 * the one thing Maps shows too small to read at arm's length on a moving bike. Drag it anywhere, tap
 * it to come back to the dashboard.
 */
class SpeedOverlay(
    private val context: Context,
    private val onTap: () -> Unit,
    private val onMoved: (x: Int, y: Int) -> Unit,
) {
    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var view: View? = null
    private var host: OverlayHost? = null
    private var params: WindowManager.LayoutParams? = null

    val showing: Boolean get() = view != null

    fun show(x: Int, y: Int) {
        if (view != null) return
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "No overlay permission; skipping the speed box")
            return
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }

        val overlayHost = OverlayHost()
        val composeView = ComposeView(context).apply {
            setContent {
                val state by RideRepository.state.collectAsState()
                RideDashTheme(night = state.night) {
                    SpeedBox(
                        state = state,
                        onDrag = { dx, dy -> moveBy(dx, dy) },
                        onDragEnd = { params?.let { onMoved(it.x, it.y) } },
                        onTap = onTap,
                    )
                }
            }
        }
        overlayHost.attachTo(composeView)

        runCatching { windowManager.addView(composeView, layoutParams) }
            .onFailure {
                Log.e(TAG, "Could not add the overlay", it)
                overlayHost.detach()
                return
            }

        view = composeView
        host = overlayHost
        params = layoutParams
    }

    fun hide() {
        val current = view ?: return
        runCatching { windowManager.removeView(current) }
        host?.detach()
        view = null
        host = null
        params = null
    }

    private fun moveBy(dx: Float, dy: Float) {
        val current = view ?: return
        val layoutParams = params ?: return
        layoutParams.x = (layoutParams.x + dx).roundToInt().coerceAtLeast(0)
        layoutParams.y = (layoutParams.y + dy).roundToInt().coerceAtLeast(0)
        runCatching { windowManager.updateViewLayout(current, layoutParams) }
    }

    companion object {
        private const val TAG = "RideDash/Overlay"
    }
}

@Composable
private fun SpeedBox(
    state: RideState,
    onDrag: (Float, Float) -> Unit,
    onDragEnd: () -> Unit,
    onTap: () -> Unit,
) {
    val colors = rideColors

    Box(
        modifier = Modifier
            .size(150.dp)
            .shadow(12.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(colors.bg)
            .border(3.dp, colors.accent, RoundedCornerShape(22.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = onDragEnd,
                    onDrag = { _, dragAmount -> onDrag(dragAmount.x, dragAmount.y) },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = Formatters.speed(state.speedKmh, state.speedValid && state.gpsFix),
                style = numberStyle(72.sp, FontWeight.ExtraBold, italic = true),
                color = colors.fg,
            )
            Text(
                text = "km/h",
                style = numberStyle(18.sp, FontWeight.SemiBold),
                color = colors.sub,
            )
            Text(
                text = "‹ Dashboard",
                style = numberStyle(16.sp, FontWeight.SemiBold),
                color = colors.accent,
            )
        }
    }
}
