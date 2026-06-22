package io.mobilegrpah.ai

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = sayHello(platform.name)
}
