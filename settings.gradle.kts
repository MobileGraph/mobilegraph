rootProject.name = "mobilegraph-project"
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
include(":foundation:mobilegraph-state")
include(":foundation:mobilegraph-checkpoint")
include(":intelligence:mobilegraph-models")
include(":intelligence:mobilegraph-prompts")
include(":intelligence:mobilegraph-parsers")
include(":intelligence:mobilegraph-mcp")
include(":intelligence:mobilegraph-tools")
include(":intelligence:mobilegraph-skills")
include(":intelligence:mobilegraph-rag")
include(":intelligence:mobilegraph-graph")
include(":intelligence:mobilegraph-agents")
include(":knowledge:mobilegraph-documents")
include(":knowledge:mobilegraph-embeddings")
include(":knowledge:mobilegraph-vectorstores")
include(":knowledge:mobilegraph-retrieval")
include(":mobilegraph-sdk")
include(":shared")
include(":androidApp")
