import dev.extframework.gradle.common.*
import dev.extframework.gradle.common.dm.artifactResolver
import dev.extframework.gradle.common.dm.jobs
import dev.extframework.gradle.publish.ExtensionPublication
import kotlin.jvm.java

plugins {
    id("dev.extframework")
}

version = "1.0.2-BETA"

extension {
    model {
        attribute("unloadable", false)
    }
    partitions {
        tweaker {
            tweakerClass = "dev.extframework.core.main.MainPartitionTweaker"
            dependencies {
                toolingApi()
                implementation(project(":entrypoint"))
                boot()
                jobs()
                artifactResolver()
                archives()
                commonUtil()
                objectContainer()
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            }
        }
        gradle {
            entrypointClass = "dev.extframework.core.main.MainGradlePlugin"
            dependencies {
                toolingApi()
                implementation(gradleApi())
                jobs()
                boot()
                implementation("dev.extframework:gradle-api:1.0.3-BETA")
                commonUtil()
                objectContainer()
            }
        }
    }

    metadata {
        name = "Main partition"
        description = "An extension that adds a main partition for loading environment agnostic code"
        app = "minecraft"
    }
}

publishing {
    publications {
        create("prod", ExtensionPublication::class.java)
    }
    repositories {
        maven {
            url = uri("https://repo.extframework.dev")
            credentials {
                password = properties["creds.ext.key"] as? String
            }
        }
    }
}