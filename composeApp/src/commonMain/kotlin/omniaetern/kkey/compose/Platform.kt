package omniaetern.kkey.compose

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform