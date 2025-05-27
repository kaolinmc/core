pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.extframework.dev/releases")
        }
        maven {
            url = uri("https://maven.extframework.dev/snapshots")
        }
        gradlePluginPortal()
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "core"

include(":tests")

include("main")
include("app")
include("entrypoint")
include("instrument")
include("capability")
include("app:api")
findProject(":app:api")?.name = "app-api"
include("minecraft")
include("minecraft:api")
findProject(":minecraft:api")?.name = "minecraft-api"

include(":minecraft:client")

include("minecraft:client:api")
findProject(":minecraft:client:api")?.name = "client-api"
