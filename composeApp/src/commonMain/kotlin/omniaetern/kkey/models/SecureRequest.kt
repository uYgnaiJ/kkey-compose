package omniaetern.kkey.models

import kotlinx.serialization.Serializable

@Serializable
data class SecureRequest(
    val encryptedData: String,          // Base64 encoded encrypted JSON
    val iv: String,                     // Base64 encoded Initialization Vector
    val encryptedKey: String? = null    // Optional: RSA-encrypted AES key if performing handshake
)
