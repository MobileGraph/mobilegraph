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
    alias(libs.plugins.mavenPublish) apply false
}

allprojects {
    group = "io.mobilegraph"
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

    plugins.withId("com.vanniktech.maven.publish") {

        extensions.configure<com.vanniktech.maven.publish.MavenPublishBaseExtension> {

            publishToMavenCentral()

            signAllPublications()

            coordinates(
                "io.github.mobilegraph",
                project.name,
                rootProject.version.toString()
            )

            pom {
                name.set(project.name)
                description.set(project.description ?: "MobileGraph module")

                inceptionYear.set("2026")

                url.set("https://github.com/MobileGraph/mobilegraph")

                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("mobilegraph")
                        name.set("Bharat Hazarika")
                        email.set("bharatjyotih2@gmail.com")
                    }
                }

                scm {
                    url.set("https://github.com/MobileGraph/mobilegraph")
                    connection.set("scm:git:https://github.com/MobileGraph/mobilegraph.git")
                    developerConnection.set("scm:git:ssh://git@github.com/MobileGraph/mobilegraph.git")
                }
            }
        }
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        val kotlin = extensions.getByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()
        kotlin.withSourcesJar()

        val dokkaGenerateHtml = tasks.named("dokkaGenerateHtml")
        tasks.register<Jar>("javadocJar") {
            archiveClassifier.set("javadoc")
            from(dokkaGenerateHtml)
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
