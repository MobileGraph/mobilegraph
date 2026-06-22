package io.mobilegraph.core.context

import android.content.Context

/**
 * A wrapper for Android Context to be stored in the environment.
 */
data class AndroidContext(
    val context: Context,
)
