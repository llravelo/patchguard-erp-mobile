package com.patchguard.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patchguard.app.camera.CameraManager
import com.patchguard.app.capture.FrameBuffer
import com.patchguard.app.capture.RecordingCoordinator
import com.patchguard.app.location.LocationProvider
import com.patchguard.app.ui.capture.CaptureScreen
import com.patchguard.app.ui.capture.CaptureViewModel
import com.patchguard.app.ui.login.LoginScreen
import com.patchguard.app.ui.login.LoginViewModel

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* permissions granted/denied — camera and location are guarded by OS UI */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionLauncher.launch(
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
        )

        val app = application as PatchGuardApp

        setContent {
            var isLoggedIn by rememberSaveable {
                mutableStateOf(app.apiConfig.isTestMode || app.credentialRepository.load() != null)
            }

            if (isLoggedIn) {
                val captureVm = viewModel<CaptureViewModel> {
                    val coordinator = RecordingCoordinator(
                        cameraManager = CameraManager(applicationContext),
                        locationProvider = LocationProvider(applicationContext),
                        frameBuffer = FrameBuffer(app.batchSize),
                        batchUploader = app.batchUploader,
                    )
                    CaptureViewModel(coordinator)
                }
                CaptureScreen(viewModel = captureVm)
            } else {
                val loginVm = viewModel<LoginViewModel> {
                    LoginViewModel(
                        authService = app.authService,
                        credentialRepository = app.credentialRepository,
                        onTokenReceived = { token ->
                            app.tokenStore.accessToken = token
                            isLoggedIn = true
                        },
                    )
                }
                val uiState by loginVm.uiState.collectAsState()
                LoginScreen(uiState = uiState, onLogin = loginVm::login)
            }
        }
    }
}
