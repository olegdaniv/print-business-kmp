package com.printbusinesskmp.auth

internal expect object GoogleIdentityService {
    fun initialize(clientId: String, onCredential: (String) -> Unit): Boolean
    fun promptSignIn(): Boolean
    fun readClientId(): String?
}
