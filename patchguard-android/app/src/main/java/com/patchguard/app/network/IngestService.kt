package com.patchguard.app.network

import android.util.Log
import com.patchguard.app.capture.FrameBuffer
import com.patchguard.app.data.CredentialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

interface BatchUploader {
    suspend fun upload(batch: List<FrameBuffer.Frame>): Result<Unit>
}

class PatchGuardIngestService(
    private val config: ApiConfig,
    private val httpClient: OkHttpClient,
    private val credentialRepository: CredentialRepository,
    private val tokenStore: TokenStore,
    private val authService: AuthService,
) : BatchUploader {

    private val json = Json { encodeDefaults = false }

    override suspend fun upload(batch: List<FrameBuffer.Frame>): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            if (!config.isTestMode) ensureToken()
            sendBatch(batch)
            if (!config.isTestMode) triggerAnalysis()
        }
    }

    private suspend fun ensureToken() {
        if (tokenStore.accessToken != null) return
        val credentials = credentialRepository.load() ?: throw IOException("No stored credentials")
        tokenStore.accessToken = authService.login(credentials.email, credentials.password)
    }

    private fun sendBatch(batch: List<FrameBuffer.Frame>) {
        val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
        for (frame in batch) {
            multipart.addFormDataPart(
                "files",
                frame.metadata.filename,
                frame.jpeg.toRequestBody("image/jpeg".toMediaType()),
            )
        }
        multipart.addFormDataPart("items_json", json.encodeToString(batch.map { it.metadata }))

        val request = Request.Builder()
            .url("${config.baseUrl}/api/v1/images/batch")
            .post(multipart.build())
            .apply { tokenStore.accessToken?.let { header("Authorization", "Bearer $it") } }
            .build()

        val response = httpClient.newCall(request).execute()
        if (response.code == 401) tokenStore.clear()
        val expected = if (config.isTestMode) 200 else 201
        if (response.code != expected) throw IOException("Upload failed: HTTP ${response.code}")
    }

    private fun triggerAnalysis() {
        runCatching {
            val request = Request.Builder()
                .url("${config.baseUrl}/api/v1/analysis/trigger")
                .post("".toRequestBody())
                .apply { tokenStore.accessToken?.let { header("Authorization", "Bearer $it") } }
                .build()
            httpClient.newCall(request).execute()
        }.onFailure { Log.w(TAG, "Analysis trigger failed: ${it.message}") }
    }

    companion object {
        private const val TAG = "IngestService"
    }
}
