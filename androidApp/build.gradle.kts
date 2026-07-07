import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(libs.androidx.material3)
    implementation(projects.shared)
    implementation(projects.foundation.mobilegraphCore)
    implementation(projects.foundation.mobilegraphState)
    implementation(projects.foundation.mobilegraphCheckpoint)
    implementation(projects.intelligence.mobilegraphModels)
    implementation(projects.intelligence.mobilegraphPrompts)
    implementation(projects.intelligence.mobilegraphParsers)
    implementation(projects.intelligence.mobilegraphTools)
    implementation(projects.intelligence.mobilegraphRag)
    implementation(projects.intelligence.mobilegraphGraph)
    implementation(projects.intelligence.mobilegraphAgents)
    implementation(projects.knowledge.mobilegraphDocuments)
    implementation(projects.knowledge.mobilegraphEmbeddings)
    implementation(projects.knowledge.mobilegraphRetrieval)
    implementation(projects.knowledge.mobilegraphVectorstores)

    implementation(libs.sqldelight.android.driver)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.jsoup)
    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    implementation(libs.pdfbox.android)
}

android {
    namespace = "io.mobilegraph.ai"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "io.mobilegraph.ai"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0"

        val properties =
            org.jetbrains.kotlin.konan.properties.Properties().apply {
                val propertiesFile = rootProject.file("local.properties")
                if (propertiesFile.exists()) {
                    propertiesFile.inputStream().use { load(it) }
                }
            }

        // 2. Fetch the key (fall back to empty string if missing)
        val openAiApiKey = properties.getProperty("open_ai_api") ?: ""

        // 3. Expose the key to your Kotlin/Java code
        buildConfigField("String", "OPEN_AI_API_KEY", "\"$openAiApiKey\"")
    }
    buildFeatures {
        // Required in newer AGP versions to generate the BuildConfig class
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
