package omniaetern.kkey.compose

class JsPlatform: Platform {
    override val name: String = "Web with Kotlin/JS"
    override fun decrypt(encryptedBase64: String, ivBase64: String, password: String): String = ""
    override fun encrypt(data: String, password: String): Pair<String, String> = Pair("", "")
}

actual fun getPlatform(): Platform = JsPlatform()