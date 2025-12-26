package omniaetern.kkey.compose

interface Platform {
    val name: String
    fun decrypt(encryptedBase64: String, ivBase64: String, password: String): String
    fun encrypt(data: String, password: String): Pair<String, String> // returns encryptedData, iv
}

expect fun getPlatform(): Platform