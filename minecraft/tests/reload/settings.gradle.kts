pluginManagement {
    repositories {
        mavenLocal()
        maven {
            url = uri("https://maven.kaolinmc.com/releases")
        }
        maven {
            url = uri("https://maven.kaolinmc.com/snapshots")
        }
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "reload"

include("target-app")
