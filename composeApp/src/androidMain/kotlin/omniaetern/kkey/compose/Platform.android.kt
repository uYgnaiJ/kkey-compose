package omniaetern.kkey.compose

import android.os.Build
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"

    override fun decrypt(encryptedBase64: String, ivBase64: String, password: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val secretKey = digest.digest(password.toByteArray())
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = Base64.decode(ivBase64, Base64.DEFAULT)
            val gcmSpec = GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(secretKey, "AES"), gcmSpec)
            
            val decodedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes)
        } catch (e: Exception) {
            "Decryption Error: ${e.message}"
        }
    }

    override fun encrypt(data: String, password: String): Pair<String, String> {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val secretKey = digest.digest(password.toByteArray())

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12)
            java.security.SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(128, iv)

            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(secretKey, "AES"), gcmSpec)

            val encryptedBytes = cipher.doFinal(data.toByteArray())
            val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

            Pair(encryptedBase64, ivBase64)
        } catch (e: Exception) {
            Pair("", "")
        }
    }
}

actual fun getPlatform(): Platform = AndroidPlatform()