package com.patchguard.app

import android.app.Application
import com.patchguard.app.data.CredentialRepository
import com.patchguard.app.data.EncryptedCredentialStore
import com.patchguard.app.network.ApiConfig
import com.patchguard.app.network.AuthService
import com.patchguard.app.network.BatchUploader
import com.patchguard.app.network.InMemoryTokenStore
import com.patchguard.app.network.PatchGuardAuthService
import com.patchguard.app.network.PatchGuardIngestService
import com.patchguard.app.network.TokenStore
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class PatchGuardApp : Application() {

    lateinit var apiConfig: ApiConfig
        private set
    lateinit var credentialRepository: CredentialRepository
        private set
    lateinit var tokenStore: TokenStore
        private set
    lateinit var authService: AuthService
        private set
    lateinit var batchUploader: BatchUploader
        private set
    var batchSize: Int = 10
        private set

    override fun onCreate() {
        super.onCreate()

        val isTestMode = resources.getBoolean(R.bool.test_mode)
        val baseUrl = (if (isTestMode)
            getString(R.string.mock_server_base_url)
        else
            getString(R.string.server_base_url)).trimEnd('/')

        apiConfig = ApiConfig(isTestMode = isTestMode, baseUrl = baseUrl)
        batchSize = resources.getInteger(R.integer.batch_size)

        credentialRepository = EncryptedCredentialStore(this)
        tokenStore = InMemoryTokenStore()

        val httpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        authService = PatchGuardAuthService(apiConfig, httpClient)
        batchUploader = PatchGuardIngestService(
            config = apiConfig,
            httpClient = httpClient,
            credentialRepository = credentialRepository,
            tokenStore = tokenStore,
            authService = authService,
        )
    }
}
