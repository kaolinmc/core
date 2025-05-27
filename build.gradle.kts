import dev.extframework.gradle.common.extFramework

plugins {
    kotlin("jvm") version "2.0.21"
    id("dev.extframework.common") version "1.0.52" apply false
    id("dev.extframework") version "1.3.1" apply false
}

group = "dev.extframework.extension"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
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
        mavenLocal()
    }

    dependencies {
//        toolingApi(version = "1.0.8-SNAPSHOT")
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