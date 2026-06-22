package io.mobilegrpah.ai

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
