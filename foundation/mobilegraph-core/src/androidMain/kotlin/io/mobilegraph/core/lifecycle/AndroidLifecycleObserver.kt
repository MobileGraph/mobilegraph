package io.mobilegraph.core.lifecycle

import android.Manifest
import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Android-specific implementation that bridges [androidx.lifecycle] and system events
 * to MobileGraph's [LifecycleRegistry].
 */
class AndroidLifecycleObserver(
    private val context: Context,
    private val registry: LifecycleRegistry,
) : DefaultLifecycleObserver,
    LifecycleObserver,
    ComponentCallbacks2 {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                registry.onEvent(LifecycleEvent.NetworkRestored)
            }

            override fun onLost(network: Network) {
                registry.onEvent(LifecycleEvent.NetworkLost)
            }
        }

    override fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        context.registerComponentCallbacks(this)

        val request =
            NetworkRequest
                .Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    override fun stop() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        context.unregisterComponentCallbacks(this)
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    override fun onStart(owner: LifecycleOwner) {
        registry.onEvent(LifecycleEvent.EnterForeground)
    }

    override fun onStop(owner: LifecycleOwner) {
        registry.onEvent(LifecycleEvent.EnterBackground)
    }

    // --- ComponentCallbacks2 for "Suspended" ---

    override fun onTrimMemory(level: Int) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            registry.onEvent(LifecycleEvent.Suspended)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onLowMemory() {
        registry.onEvent(LifecycleEvent.Suspended)
    }
}
