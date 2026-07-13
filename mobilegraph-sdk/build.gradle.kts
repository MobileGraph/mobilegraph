import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    `maven-publish`
}

group = "com.github.MobileGraph"
// version is inherited from root project

kotlin {
    android {
        namespace = "io.mobilegraph.sdk"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // Export all core modules
            api(project(":foundation:mobilegraph-core"))
            api(project(":foundation:mobilegraph-state"))
            api(project(":foundation:mobilegraph-checkpoint"))
            
            api(project(":intelligence:mobilegraph-models"))
            api(project(":intelligence:mobilegraph-agents"))
            api(project(":intelligence:mobilegraph-graph"))
            api(project(":intelligence:mobilegraph-tools"))
            api(project(":intelligence:mobilegraph-parsers"))
            api(project(":intelligence:mobilegraph-prompts"))
            api(project(":intelligence:mobilegraph-rag"))
            
            api(project(":knowledge:mobilegraph-documents"))
            api(project(":knowledge:mobilegraph-embeddings"))
            api(project(":knowledge:mobilegraph-retrieval"))
            api(project(":knowledge:mobilegraph-vectorstores"))
        }
    }
}
