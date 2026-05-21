package com.patchguard.app.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

class FrameProcessor {

    fun process(imageProxy: ImageProxy): ByteArray {
        val bitmap = imageProxy.toBitmap()

        val upright = applyRotation(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())
        if (upright !== bitmap) bitmap.recycle()

        val cropped = centerCrop(upright)
        if (cropped !== upright) upright.recycle()

        return ByteArrayOutputStream().also { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            cropped.recycle()
        }.toByteArray()
    }

    private fun applyRotation(src: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return src
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    // Center-crops to a 1024×1024 square — mirrors the iOS CIImage crop from 1920×1080.
    private fun centerCrop(src: Bitmap): Bitmap {
        val side = minOf(src.width, src.height)
        val x = (src.width - side) / 2
        val y = (src.height - side) / 2
        val cropped = Bitmap.createBitmap(src, x, y, side, side)
        if (side == TARGET_SIZE) return cropped
        val scaled = Bitmap.createScaledBitmap(cropped, TARGET_SIZE, TARGET_SIZE, true)
        cropped.recycle()
        return scaled
    }

    companion object {
        private const val TARGET_SIZE = 1024
        private const val JPEG_QUALITY = 85
    }
}
