import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    `maven-publish`
    alias(libs.plugins.dokka)

    alias(libs.plugins.mavenPublish)
}

// group is inherited from root project
// version is inherited from root project

kotlin {
    android {
        namespace = "io.mobilegraph.graph"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_11 }
    }
    jvm()

    val hostOs = System.getProperty("os.name")
    if (hostOs == "Mac OS X") {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":foundation:mobilegraph-core"))
            implementation(project(":foundation:mobilegraph-state"))
            implementation(project(":foundation:mobilegraph-checkpoint"))
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
