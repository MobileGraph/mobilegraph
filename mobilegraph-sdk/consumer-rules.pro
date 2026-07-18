# Ktor & Serialization
-keepattributes Signature, *Annotation*, InnerClasses
-keep class io.ktor.** { *; }
-keep class kotlinx.serialization.json.** { *; }

# MobileGraph Models (To prevent JSON mapping failures)
-keep class io.mobilegraph.models.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
