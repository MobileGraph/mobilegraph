package io.mobilegraph.ai

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
