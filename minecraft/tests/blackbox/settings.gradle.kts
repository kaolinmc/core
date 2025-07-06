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
rootProject.name = "blackbox"

include(":app")
//findProject(":app")?.run {
//    projectDir = file("minecraft/tests/app")
//    name = "blackbox-app"
//}