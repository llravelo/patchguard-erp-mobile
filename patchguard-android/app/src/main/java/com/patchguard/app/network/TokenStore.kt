package com.patchguard.app.network

interface TokenStore {
    var accessToken: String?
    fun clear()
}

class InMemoryTokenStore : TokenStore {
    @Volatile override var accessToken: String? = null
    override fun clear() { accessToken = null }
}
