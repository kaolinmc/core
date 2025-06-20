import dev.extframework.gradle.common.*
import dev.extframework.gradle.publish.ExtensionPublication

plugins {
    id("dev.extframework")
}

version = "1.0.3-BETA"

extension {
    model {
        attribute("unloadable", false)
    }
    partitions {
        tweaker {
            tweakerClass = "dev.extframework.core.main.MainPartitionTweaker"
            dependencies {
                implementation(project(":entrypoint"))

                implementation(toolingApi())
                implementation(boot())
                implementation(artifactResolver())
                implementation(archives())
                implementation(commonUtil())
                implementation(objectContainer())

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            }
        }
        gradle {
            entrypointClass = "dev.extframework.core.main.MainGradlePlugin"
            dependencies {
                implementation(toolingApi())
                implementation(gradleApi())
                implementation(boot())
                implementation("dev.extframework:gradle-api:1.1-BETA")
                implementation(commonUtil())
                implementation(objectContainer())
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