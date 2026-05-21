package com.patchguard.app.data

import androidx.annotation.OptIn
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.InternalSerializationApi

@OptIn(InternalSerializationApi::class)
@Serializable
data class ImageMetadata(
    val filename: String,
    val latitude: Double,
    val longitude: Double,
    @SerialName("captured_at") val capturedAt: String,
    val heading: Double? = null,
    val altitude: Double? = null,
    @SerialName("gps_accuracy") val gpsAccuracy: Double? = null,
)
