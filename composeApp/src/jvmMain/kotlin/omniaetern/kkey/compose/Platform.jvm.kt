package omniaetern.kkey.compose

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"

    override fun decrypt(encryptedBase64: String, ivBase64: String, password: String): String {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val secretKey = digest.digest(password.toByteArray())
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, Base64.getDecoder().decode(ivBase64))
            
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(secretKey, "AES"), gcmSpec)
            
            val decodedBytes = Base64.getDecoder().decode(encryptedBase64)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes)
        } catch (e: Exception) {
            return "Decryption Error: ${e.message}"
        }
    }

    override fun encrypt(data: String, password: String): Pair<String, String> {
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val secretKey = digest.digest(password.toByteArray())

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12)
            java.security.SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(128, iv)

            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(secretKey, "AES"), gcmSpec)

            val encryptedBytes = cipher.doFinal(data.toByteArray())
            val encryptedBase64 = Base64.getEncoder().encodeToString(encryptedBytes)
            val ivBase64 = Base64.getEncoder().encodeToString(iv)

            return Pair(encryptedBase64, ivBase64)
        } catch (e: Exception) {
            println("Encryption Error: ${e.message}")
            return Pair("", "")
        }
    }
}

actual fun getPlatform(): Platform = JVMPlatform()