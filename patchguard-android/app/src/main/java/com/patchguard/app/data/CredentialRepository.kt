package com.patchguard.app.data

interface CredentialRepository {
    fun save(email: String, password: String)
    fun load(): Credentials?
    fun clear()
}
