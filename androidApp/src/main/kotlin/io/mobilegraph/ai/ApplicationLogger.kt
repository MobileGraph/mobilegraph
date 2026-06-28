/*
* MobileGraph
*
* Copyright (c) 2026-present The MobileGraph Authors
*
* Licensed under the Apache License, Version 2.0.
* See LICENSE for details.
*/

package io.mobilegraph.ai

import android.util.Log
import io.mobilegraph.models.middleware.MobileGraphLogger

class ApplicationLogger : MobileGraphLogger {
    override fun log(
        message: String,
        severity: MobileGraphLogger.Severity,
    ) {
        when (severity) {
            MobileGraphLogger.Severity.VERBOSE -> Log.v("MobileGraph", message)
            MobileGraphLogger.Severity.DEBUG -> Log.d("MobileGraph", message)
            MobileGraphLogger.Severity.INFO -> Log.i("MobileGraph", message)
            MobileGraphLogger.Severity.WARN -> Log.w("MobileGraph", message)
            MobileGraphLogger.Severity.ERROR -> Log.e("MobileGraph", message)
        }
    }
}
