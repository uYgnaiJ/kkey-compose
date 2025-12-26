package omniaetern.kkey.compose

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override fun decrypt(encryptedBase64: String, ivBase64: String, password: String): String {
        return "Decryption not yet implemented for iOS"
    }

    override fun encrypt(data: String, password: String): Pair<String, String> {
        return Pair("", "Encryption not yet implemented for iOS")
    }
}

actual fun getPlatform(): Platform = IOSPlatform()