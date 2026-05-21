package com.patchguard.app.ui.capture

import android.location.Location
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.patchguard.app.capture.RecordingCoordinator
import kotlinx.coroutines.flow.StateFlow

class CaptureViewModel(private val coordinator: RecordingCoordinator) : ViewModel() {

    val isRunning: StateFlow<Boolean> = coordinator.isRunning
    val isCalibrated: StateFlow<Boolean> = coordinator.isCalibrated
    val frameCount: StateFlow<Int> = coordinator.frameCount
    val location: StateFlow<Location?> = coordinator.location

    fun setup(lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        coordinator.setup(lifecycleOwner, surfaceProvider)
    }

    fun calibrate() = coordinator.calibrate()
    fun resetCalibration() = coordinator.resetCalibration()
    fun setSamplingRate(fps: Int) = coordinator.setSamplingRate(fps)
    fun startRecording() = coordinator.startRecording()
    fun stopRecording() = coordinator.stopRecording()
    fun handleBackground() = coordinator.handleBackground()
}
