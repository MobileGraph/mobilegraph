package io.mobilegraph.core.annotations

/**
 * Marks declarations that are internal to the MobileGraph SDK.
 *
 * These APIs are not intended for use by application developers and may change
 * or be removed without notice in future versions.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal MobileGraph API. It should not be used in application code.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class InternalMobileGraphApi
