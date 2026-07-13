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
        namespace = "io.mobilegraph.rag"
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
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            api(project(":knowledge:mobilegraph-retrieval"))
            api(project(":intelligence:mobilegraph-models"))
            api(project(":intelligence:mobilegraph-prompts"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
