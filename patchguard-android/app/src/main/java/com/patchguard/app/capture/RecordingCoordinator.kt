package com.patchguard.app.capture

import android.location.Location
import android.util.Log
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import com.patchguard.app.camera.CameraManager
import com.patchguard.app.data.ImageMetadata
import com.patchguard.app.location.LocationProvider
import com.patchguard.app.network.BatchUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class RecordingCoordinator(
    private val cameraManager: CameraManager,
    private val locationProvider: LocationProvider,
    private val frameBuffer: FrameBuffer,
    private val batchUploader: BatchUploader,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    private val _frameCount = MutableStateFlow(0)
    val frameCount: StateFlow<Int> = _frameCount

    val isCalibrated: StateFlow<Boolean> = cameraManager.isCalibrated
    val location: StateFlow<Location?> = locationProvider.location

    private val iso8601 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun setup(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        frameBuffer.onBatchReady = { batch ->
            scope.launch {
                batchUploader.upload(batch).onFailure {
                    Log.e(TAG, "Upload failed: ${it.message}")
                }
            }
        }
        cameraManager.bind(lifecycleOwner, surfaceProvider)
    }

    fun calibrate() = cameraManager.calibrate()
    fun resetCalibration() = cameraManager.resetCalibration()
    fun setSamplingRate(fps: Int) = cameraManager.setSamplingRate(fps)

    fun startRecording() {
        _frameCount.value = 0
        _isRunning.value = true
        locationProvider.start()
        cameraManager.onFrame = ::handleFrame
    }

    fun stopRecording() {
        cameraManager.onFrame = null
        _isRunning.value = false
        locationProvider.stop()
        frameBuffer.flush()
    }

    fun handleBackground() {
        cameraManager.onFrame = null
        _isRunning.value = false
        locationProvider.stop()
        frameBuffer.clear()
    }

    private fun handleFrame(jpeg: ByteArray) {
        val loc = locationProvider.location.value
        val metadata = ImageMetadata(
            filename = "frame_${System.currentTimeMillis()}.jpg",
            latitude = loc?.latitude ?: 0.0,
            longitude = loc?.longitude ?: 0.0,
            capturedAt = iso8601.format(Date()),
            heading = loc?.bearing?.toDouble()?.takeIf { it >= 0 },
            altitude = loc?.altitude,
            gpsAccuracy = loc?.accuracy?.toDouble()?.takeIf { it >= 0 },
        )
        _frameCount.value += 1
        frameBuffer.add(FrameBuffer.Frame(jpeg, metadata))
    }

    companion object {
        private const val TAG = "RecordingCoordinator"
    }
}
