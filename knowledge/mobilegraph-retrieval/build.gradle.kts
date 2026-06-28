import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    android {
        namespace = "io.mobilegraph.retrieval"
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
            api(project(":knowledge:mobilegraph-documents"))
            api(project(":knowledge:mobilegraph-vectorstores"))
            implementation(project(":intelligence:mobilegraph-parsers"))
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
