rootProject.name = "MobileGraphSDK"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":foundation:mobilegraph-core")
include(":intelligence:mobilegraph-models")
include(":intelligence:mobilegraph-prompts")
include(":intelligence:mobilegraph-parsers")
include(":intelligence:mobilegraph-tools")
include(":shared")
include(":androidApp")
