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
        namespace = "io.mobilegraph.prompts"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
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
            api(projects.foundation.mobilegraphCore)
            implementation(projects.intelligence.mobilegraphModels)
            implementation(projects.intelligence.mobilegraphTools)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
