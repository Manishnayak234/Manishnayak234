package com.manish.ridedash.ui.record

import android.Manifest
import android.content.pm.PackageManager
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.manish.ridedash.R
import com.manish.ridedash.record.RecordingController
import com.manish.ridedash.ui.dashboard.DashButton
import com.manish.ridedash.ui.theme.Warn
import com.manish.ridedash.ui.theme.labelStyle
import com.manish.ridedash.ui.theme.numberStyle
import com.manish.ridedash.ui.theme.rideColors
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Recording mode: the camera preview fills the screen, with the speed panel already composited into it
 * by [RecordingController]. What is on screen is what lands in the file, so there is nothing to imagine
 * — the rider can see the framing and the numbers before pressing record.
 *
 * The controls stay deliberately few and large: record/stop, flip the camera, and back.
 */
@Composable
fun RecordingScreen(
    controller: RecordingController,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = rideColors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by controller.state.collectAsStateWithLifecycle()

    var frontCamera by remember { mutableStateOf(true) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FIT_CENTER }
    }

    val hasCamera = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
    }
    val hasMicrophone = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
    }

    // Re-binds when the camera is flipped; the preview keeps its surface either way.
    LaunchedEffect(frontCamera, hasCamera) {
        if (!hasCamera) return@LaunchedEffect
        controller.bind(lifecycleOwner, frontCamera)?.setSurfaceProvider(previewView.surfaceProvider)
    }

    DisposableEffect(Unit) {
        onDispose { controller.release() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        if (hasCamera) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                text = stringResource(R.string.record_no_camera),
                style = numberStyle(24.sp, FontWeight.Bold),
                color = colors.sub,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
        }

        if (state.recording) {
            RecordingChip(
                elapsedMs = state.elapsedMs,
                controller = controller,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            )
        }

        state.error?.let { message ->
            Text(
                text = message,
                style = labelStyle,
                color = Warn,
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DashButton(
                label = stringResource(R.string.record_back),
                onClick = onBack,
                modifier = Modifier.width(150.dp),
            )
            DashButton(
                label = stringResource(
                    if (frontCamera) R.string.record_flip_to_road else R.string.record_flip_to_rider,
                ),
                onClick = { if (!state.recording) frontCamera = !frontCamera },
                modifier = Modifier.width(190.dp),
                labelColor = if (state.recording) colors.sub else colors.fg,
            )
            DashButton(
                label = stringResource(
                    if (state.recording) R.string.record_stop else R.string.record_start,
                ),
                onClick = {
                    if (state.recording) controller.stop() else controller.start(hasMicrophone)
                },
                modifier = Modifier.width(190.dp),
                labelColor = if (state.recording) Warn else colors.accent,
            )
        }

        if (!hasMicrophone && hasCamera) {
            Text(
                text = stringResource(R.string.record_no_microphone),
                style = labelStyle,
                color = colors.sub,
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
            )
        }
    }
}

/** The red dot and the clock, so there is never any doubt that it is running. */
@Composable
private fun RecordingChip(
    elapsedMs: Long,
    controller: RecordingController,
    modifier: Modifier = Modifier,
) {
    val colors = rideColors
    var shown by remember { mutableStateOf(elapsedMs) }

    // The camera reports progress in bursts; a one second tick keeps the clock honest.
    LaunchedEffect(Unit) {
        while (true) {
            shown = controller.elapsedMs()
            delay(500L)
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.bg)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Warn),
        )
        Text(
            text = "REC " + elapsed(shown),
            style = numberStyle(22.sp, FontWeight.Bold),
            color = colors.fg,
        )
    }
}

private fun elapsed(ms: Long): String {
    val seconds = ms / 1000
    return String.format(Locale.US, "%d:%02d", seconds / 60, seconds % 60)
}
