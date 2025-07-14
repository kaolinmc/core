import com.kaolinmc.gradle.common.*


plugins {
    kotlin("jvm") version "2.0.21"
    id("com.kaolinmc.common") version "0.1.5"
    id("kaolin.kiln") version "0.1.5"
}

tasks.wrapper {
    gradleVersion = "8.14.2"
}

logger.lifecycle(gradle.gradleHomeDir.toString())

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
    apply(plugin = "com.kaolinmc.common")

    group = "com.kaolinmc.core"

    repositories {
        mavenCentral()
        kaolin()
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