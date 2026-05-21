package com.patchguard.app.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors

class CameraManager(private val context: Context) {

    private val _isCalibrated = MutableStateFlow(false)
    val isCalibrated: StateFlow<Boolean> = _isCalibrated

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private val frameProcessor = FrameProcessor()

    private var camera: Camera? = null
    private var samplingIntervalMs: Long = 1000L
    private var lastFrameTimeMs: Long = 0L

    var onFrame: ((ByteArray) -> Unit)? = null

    fun setSamplingRate(fps: Int) {
        samplingIntervalMs = if (fps > 0) 1000L / fps else 1000L
    }

    fun bind(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(analysisExecutor, ::analyzeFrame) }

            provider.unbindAll()
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis,
            )
        }, ContextCompat.getMainExecutor(context))
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun calibrate() {
        val cam = camera ?: return
        val camera2Info = Camera2CameraInfo.from(cam.cameraInfo)

        // Use device's minimum sensitivity (ISO) — keeps image bright while locking shutter speed.
        // Mirrors iOS AVCaptureDevice.currentISO usage at calibration time.
        val sensitivityRange = camera2Info.getCameraCharacteristic(
            CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE,
        )
        val iso = sensitivityRange?.let { maxOf(it.lower, 100) } ?: 200

        Camera2CameraControl.from(cam.cameraControl).captureRequestOptions =
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                // 1/500s shutter — reduces motion blur on road surface at driving speed
                .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, EXPOSURE_1_OVER_500_NS)
                .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
                .build()

        Log.i(TAG, "Calibrated — 1/500s, ISO $iso")
        _isCalibrated.value = true
    }

    @OptIn(ExperimentalCamera2Interop::class)
    fun resetCalibration() {
        val cam = camera ?: return
        Camera2CameraControl.from(cam.cameraControl).captureRequestOptions =
            CaptureRequestOptions.Builder()
                .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                .build()
        _isCalibrated.value = false
    }

    private fun analyzeFrame(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastFrameTimeMs < samplingIntervalMs) {
            imageProxy.close()
            return
        }
        lastFrameTimeMs = now
        try {
            val jpeg = frameProcessor.process(imageProxy)
            onFrame?.invoke(jpeg)
        } catch (e: Exception) {
            Log.e(TAG, "Frame processing failed", e)
        } finally {
            imageProxy.close()
        }
    }

    companion object {
        private const val TAG = "CameraManager"
        private const val EXPOSURE_1_OVER_500_NS = 2_000_000L
    }
}
