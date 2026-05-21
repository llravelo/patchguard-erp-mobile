package com.patchguard.app.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

interface AuthService {
    suspend fun login(email: String, password: String): String
}

class PatchGuardAuthService(
    private val config: ApiConfig,
    private val httpClient: OkHttpClient,
) : AuthService {

    override suspend fun login(email: String, password: String): String = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("username", email)
            .add("password", password)
            .build()
        val request = Request.Builder()
            .url("${config.baseUrl}/api/v1/auth/login")
            .post(body)
            .build()
        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Login failed: HTTP ${response.code}")
        JSONObject(response.body!!.string()).getString("access_token")
    }
}
