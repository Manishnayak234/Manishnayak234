package com.manish.ridedash.record

import android.content.ContentValues
import android.content.Context
import android.graphics.PorterDuff
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.effects.OverlayEffect
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.manish.ridedash.data.RideRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Recording mode: the camera writes a normal video file at sensor quality, and the speed panel is
 * composited into the frames as they go past.
 *
 * The compositing is CameraX's own [OverlayEffect], pointed at both the preview and the recording, so
 * what the rider sees on the phone is exactly what lands in the file — no separate export step, and no
 * second layout to keep in sync.
 *
 * Only the panel is drawn here; [SpeedPanelPainter] decides what it says.
 */
class RecordingController(private val context: Context) {

    private val painter = SpeedPanelPainter(context)
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var overlayEffect: OverlayEffect? = null
    private var recording: Recording? = null
    private var startedAtMs = 0L

    private val _state = MutableStateFlow(RecordingState())
    val state: StateFlow<RecordingState> = _state.asStateFlow()

    /**
     * Binds the camera and hands back the preview so a `PreviewView` can show it. Call from the
     * recording screen once the camera permission is granted.
     */
    suspend fun bind(lifecycleOwner: LifecycleOwner, frontCamera: Boolean): Preview? {
        val provider = awaitCameraProvider() ?: return null
        cameraProvider = provider

        val preview = Preview.Builder().build()

        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.FHD,
                    FallbackStrategy.higherQualityOrLowerThan(Quality.HD),
                )
            )
            .build()
        val capture = VideoCapture.withOutput(recorder)
        videoCapture = capture

        val effect = OverlayEffect(
            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
            OVERLAY_QUEUE_DEPTH,
            Handler(Looper.getMainLooper()),
        ) { error -> Log.e(TAG, "Overlay effect failed", error) }

        effect.setOnDrawListener { frame ->
            val canvas = frame.overlayCanvas
            // The canvas is re-used between frames, so last frame's numbers have to go first.
            canvas.drawColor(0, PorterDuff.Mode.CLEAR)
            painter.draw(
                canvas = canvas,
                width = frame.size.width,
                height = frame.size.height,
                state = RideRepository.state.value,
                elapsedMs = elapsedMs(),
            )
            true
        }
        overlayEffect = effect

        val selector = if (frontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        val useCases = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(capture)
            .addEffect(effect)
            .build()

        return runCatching {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, useCases)
            _state.value = _state.value.copy(bound = true, error = null)
            preview
        }.getOrElse { error ->
            Log.e(TAG, "Could not bind the camera", error)
            _state.value = _state.value.copy(bound = false, error = error.message)
            null
        }
    }

    /** Starts writing to Movies/RideDash. [withAudio] needs the microphone permission. */
    fun start(withAudio: Boolean) {
        val capture = videoCapture ?: return
        if (recording != null) return

        val name = "RideDash_" +
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".mp4"
        val details = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/RideDash")
        }
        val output = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(details)
            .build()

        var pending = capture.output.prepareRecording(context, output)
        if (withAudio) {
            pending = runCatching { pending.withAudioEnabled() }
                .onFailure { Log.w(TAG, "Carrying on without audio", it) }
                .getOrDefault(pending)
        }

        startedAtMs = System.currentTimeMillis()
        recording = pending.start(mainExecutor) { event -> onRecordEvent(event) }
        _state.value = _state.value.copy(recording = true, error = null, lastFile = null)
    }

    fun stop() {
        recording?.stop()
        recording = null
        _state.value = _state.value.copy(recording = false)
    }

    /** Called when the recording screen goes away: stop, unbind, forget everything. */
    fun release() {
        stop()
        runCatching { cameraProvider?.unbindAll() }
        overlayEffect?.close()
        overlayEffect = null
        videoCapture = null
        cameraProvider = null
        _state.value = RecordingState()
    }

    /**
     * ProcessCameraProvider hands back a ListenableFuture; this is the one place we need it, so it is
     * bridged here rather than by depending on coroutines-guava.
     */
    private suspend fun awaitCameraProvider(): ProcessCameraProvider? =
        suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    val result = runCatching { future.get() }
                        .onFailure { Log.e(TAG, "Camera provider unavailable", it) }
                        .getOrNull()
                    if (continuation.isActive) continuation.resumeWith(Result.success(result))
                },
                mainExecutor,
            )
            continuation.invokeOnCancellation { future.cancel(false) }
        }

    fun elapsedMs(): Long =
        if (startedAtMs == 0L || !_state.value.recording) 0L
        else System.currentTimeMillis() - startedAtMs

    private fun onRecordEvent(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start ->
                _state.value = _state.value.copy(recording = true)

            is VideoRecordEvent.Status ->
                _state.value = _state.value.copy(elapsedMs = elapsedMs())

            is VideoRecordEvent.Finalize -> {
                val error = if (event.hasError()) {
                    Log.e(TAG, "Recording failed with code ${event.error}", event.cause)
                    "Recording failed (${event.error})"
                } else {
                    null
                }
                _state.value = _state.value.copy(
                    recording = false,
                    elapsedMs = 0L,
                    error = error,
                    lastFile = if (error == null) event.outputResults.outputUri.toString() else null,
                )
                startedAtMs = 0L
            }
        }
    }

    companion object {
        private const val TAG = "RideDash/Recording"

        /** Frames the effect may hold while the overlay is drawn; the CameraX default is 3-5. */
        private const val OVERLAY_QUEUE_DEPTH = 4
    }
}

data class RecordingState(
    val bound: Boolean = false,
    val recording: Boolean = false,
    val elapsedMs: Long = 0L,
    val lastFile: String? = null,
    val error: String? = null,
)
