package io.mobilegraph.core.annotations

/**
 * Marks a MobileGraph API as experimental.
 *
 * Experimental APIs are subject to change or removal in future releases without notice.
 * Use with caution.
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is experimental and may change in the future.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
annotation class ExperimentalMobileGraphApi
