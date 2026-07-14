plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    id("jacoco")
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.dokka) apply false
}

allprojects {
    group = "io.mobilegraph.ai"
    version = properties["version"] as String

    apply(plugin = "jacoco")

    configure<JacocoPluginExtension> {
        toolVersion = "0.8.12"
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

subprojects {
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "org.jetbrains.dokka")

    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        val kotlin = extensions.getByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()
        kotlin.withSourcesJar()

        val dokkaGenerateHtml = tasks.named("dokkaGenerateHtml")
        val javadocJar by tasks.registering(Jar::class) {
            from(dokkaGenerateHtml)
            archiveClassifier.set("javadoc")
        }

        // Attach the Javadoc JAR to all publications
        afterEvaluate {
            pluginManager.withPlugin("maven-publish") {
                extensions.configure<PublishingExtension> {
                    publications.all {
                        val publication = this
                        if (publication is MavenPublication) {
                            publication.artifact(javadocJar)
                        }
                    }
                }
            }
        }
    }

    if (project.path != ":androidApp") {
        apply(plugin = "io.gitlab.arturbosch.detekt")

        configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        }
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint()
        }
        kotlinGradle {
            target("**/*.gradle.kts")
            targetExclude("**/build/**")
            ktlint()
        }
    }

    tasks.matching { it.name == "preBuild" || it.name == "classes" }.configureEach {
        dependsOn(rootProject.tasks.named("spotlessApply"))
    }
}

// Root project spotless and detekt
apply(plugin = "com.diffplug.spotless")
apply(plugin = "io.gitlab.arturbosch.detekt")

configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
    buildUponDefaultConfig = true
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint()
    }
}

val subprojects =
    listOf(
        "foundation:mobilegraph-core",
        "intelligence:mobilegraph-models",
        "intelligence:mobilegraph-parsers",
        "intelligence:mobilegraph-prompts",
        "intelligence:mobilegraph-tools",
        "intelligence:mobilegraph-rag",
        "knowledge:mobilegraph-documents",
        "knowledge:mobilegraph-embeddings",
        "knowledge:mobilegraph-retrieval",
        "knowledge:mobilegraph-vectorstores",
    )

tasks.register<JacocoReport>("combinedTestReport") {
    dependsOn(subprojects.map { ":$it:jvmTest" })

    val classDirectories = subprojects.map { project(":$it").layout.buildDirectory.dir("classes/kotlin/jvm/main") }

    val sourceDirectories =
        subprojects.flatMap {
            listOf(
                project(":$it").projectDir.resolve("src/commonMain/kotlin"),
                project(":$it").projectDir.resolve("src/jvmMain/kotlin"),
            )
        }

    val executionData = subprojects.map { project(":$it").layout.buildDirectory.file("jacoco/jvmTest.exec") }

    this.classDirectories.setFrom(files(classDirectories))
    this.sourceDirectories.setFrom(files(sourceDirectories))
    this.executionData.setFrom(files(executionData))

    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}
