package io.mobilegraph.ai

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = sayHello(platform.name)
}
