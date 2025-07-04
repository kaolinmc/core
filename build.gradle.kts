import dev.extframework.gradle.common.*


plugins {
    kotlin("jvm") version "2.0.21"
    id("dev.extframework.common") version "1.1.2"
    id("dev.extframework") version "1.4.2"
}

tasks.wrapper {
    gradleVersion = "8.14.2"
}

val publishAll by tasks.registering {
    listOf(
        ":app",
        ":instrument",
        ":main",
        ":minecraft"
    ).forEach { project ->
        dependsOn(project(project).tasks.named("publishExtension"))
    }

    listOf(
        ":capability",
        ":entrypoint",
        ":app:app-api",
        ":minecraft:minecraft-api",
        ":minecraft:client:client-api"
    ).forEach { project ->
        dependsOn(project(project).tasks.named("publish"))
    }
}

val publishAllLocally by tasks.registering {
    listOf(
        ":app",
        ":instrument",
        ":main",
        ":minecraft"
    ).forEach { project ->
        dependsOn(project(project).tasks.named("publishToMavenLocal"))
    }

    listOf(
        ":capability",
        ":entrypoint",
        ":app:app-api",
        ":minecraft:minecraft-api",
        ":minecraft:client:client-api",
    ).forEach { project ->
        dependsOn(project(project).tasks.named("publishToMavenLocal"))
    }
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "dev.extframework.common")

    group = "dev.extframework.core"

    repositories {
        mavenCentral()
        extFramework()
    }

    kotlin {
        explicitApi()
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            freeCompilerArgs += "-Xexplicit-api=strict"
        }
    }

    kotlin {
        jvmToolchain(8)
    }
}