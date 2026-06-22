package io.mobilegraph.parsers.facade

import io.mobilegraph.core.facade.MobileGraph

/**
 * Provides access to parser-related functionality through the MobileGraph facade.
 */
val MobileGraph.parsers: MobileGraphParsers
    get() = MobileGraphParsers(this)

/**
 * Entry point for parser operations.
 */
class MobileGraphParsers(
    private val mobileGraph: MobileGraph,
) {
    // Parser-specific facade methods can be added here
}

/**
 * Global access to parsers.
 */
val MobileGraph.Companion.parsers: MobileGraphParsers
    get() = MobileGraph.instance.parsers
