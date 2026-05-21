package com.patchguard.app.ui.capture

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private val FPS_OPTIONS = listOf(1, 2, 5)

@Composable
fun CaptureScreen(viewModel: CaptureViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val isRunning by viewModel.isRunning.collectAsState()
    val isCalibrated by viewModel.isCalibrated.collectAsState()
    val frameCount by viewModel.frameCount.collectAsState()
    val location by viewModel.location.collectAsState()
    val hasGPS = location != null

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && isRunning) viewModel.handleBackground()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }.also { previewView ->
                    viewModel.setup(lifecycleOwner, previewView.surfaceProvider)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        CropOverlay(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            StatusBar(
                isRunning = isRunning,
                frameCount = frameCount,
                hasGPS = hasGPS,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp),
            )
            Controls(
                isCalibrated = isCalibrated,
                isRunning = isRunning,
                onCalibrate = viewModel::calibrate,
                onResetCalibration = viewModel::resetCalibration,
                onSetFps = viewModel::setSamplingRate,
                onToggleRecording = { if (isRunning) viewModel.stopRecording() else viewModel.startRecording() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 48.dp),
            )
        }
    }
}

@Composable
private fun StatusBar(
    isRunning: Boolean,
    frameCount: Int,
    hasGPS: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Canvas(modifier = Modifier.size(8.dp)) {
                drawCircle(if (isRunning) Color.Red else Color.White.copy(alpha = 0.5f))
            }
            Text(
                text = if (isRunning) "$frameCount frames" else "Ready",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Box(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                .padding(10.dp),
        ) {
            Text(
                text = if (hasGPS) "GPS" else "NO GPS",
                color = if (hasGPS) Color(0xFF4CAF50) else Color(0xFFFF9800),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun Controls(
    isCalibrated: Boolean,
    isRunning: Boolean,
    onCalibrate: () -> Unit,
    onResetCalibration: () -> Unit,
    onSetFps: (Int) -> Unit,
    onToggleRecording: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (!isCalibrated) {
            Button(
                onClick = onCalibrate,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
            ) {
                Text("Calibrate", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FPS_OPTIONS.forEach { fps ->
                    FpsChip(fps = fps, onClick = { onSetFps(fps) })
                }
            }
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RecordButton(isRunning = isRunning, onClick = onToggleRecording)
                if (!isRunning) {
                    TextButton(
                        onClick = onResetCalibration,
                        modifier = Modifier.align(Alignment.CenterStart),
                    ) {
                        Text("Recalibrate", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun FpsChip(fps: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
    ) {
        Text("$fps FPS", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RecordButton(isRunning: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                style = Stroke(width = 3.dp.toPx()),
            )
            if (isRunning) {
                val side = 30.dp.toPx()
                drawRect(
                    color = Color.Red,
                    topLeft = Offset((size.width - side) / 2, (size.height - side) / 2),
                    size = androidx.compose.ui.geometry.Size(side, side),
                )
            } else {
                drawCircle(color = Color.White, radius = 31.dp.toPx())
            }
        }
    }
}

// Dimmed overlay with a square crop window and corner markers — mirrors iOS ViewfinderCorners.
@Composable
private fun CropOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val side = minOf(size.width, size.height)
        val left = (size.width - side) / 2f
        val top = (size.height - side) / 2f
        val cropRect = Rect(left, top, left + side, top + side)

        val overlay = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(0f, 0f, size.width, size.height))
            addRect(cropRect)
        }
        drawPath(overlay, Color.Black.copy(alpha = 0.5f))

        val cornerLen = 28.dp.toPx()
        val strokeWidth = 2.5.dp.toPx()
        val corners = listOf(
            listOf(
                Offset(cropRect.left, cropRect.top + cornerLen),
                Offset(cropRect.left, cropRect.top),
                Offset(cropRect.left + cornerLen, cropRect.top),
            ),
            listOf(
                Offset(cropRect.right - cornerLen, cropRect.top),
                Offset(cropRect.right, cropRect.top),
                Offset(cropRect.right, cropRect.top + cornerLen),
            ),
            listOf(
                Offset(cropRect.left, cropRect.bottom - cornerLen),
                Offset(cropRect.left, cropRect.bottom),
                Offset(cropRect.left + cornerLen, cropRect.bottom),
            ),
            listOf(
                Offset(cropRect.right - cornerLen, cropRect.bottom),
                Offset(cropRect.right, cropRect.bottom),
                Offset(cropRect.right, cropRect.bottom - cornerLen),
            ),
        )
        corners.forEach { pts ->
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                lineTo(pts[1].x, pts[1].y)
                lineTo(pts[2].x, pts[2].y)
            }
            drawPath(
                path,
                Color.White.copy(alpha = 0.85f),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
    }
}
