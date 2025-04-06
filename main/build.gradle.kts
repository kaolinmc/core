import dev.extframework.gradle.common.*
import dev.extframework.gradle.common.dm.artifactResolver
import dev.extframework.gradle.common.dm.jobs

plugins {
    id("dev.extframework")
}

version = "1.0.1-BETA"

extension {
    model {
        attribute("unloadable", false)
    }
    partitions {
        tweaker {
            tweakerClass = "dev.extframework.core.main.MainPartitionTweaker"
            dependencies {
                toolingApi(version = "1.0.8-SNAPSHOT")
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
                implementation(gradleApi())
//                boot()
                jobs()
//                artifactResolver()
                implementation("dev.extframework:gradle-api:1.0-BETA")
                commonUtil()
                objectContainer()
            }
        }
    }

    metadata {
        name = "Main partition"
        description = "An extension that adds a main partition for loading environment agnostic code"
    }
}