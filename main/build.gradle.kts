import com.kaolinmc.gradle.common.*
import com.kaolinmc.kiln.publish.ExtensionPublication

plugins {
    id("kaolin.kiln")
}

version = "1.0.4-BETA"

extension {
    model {
        attribute("unloadable", false)
    }
    partitions {
        tweaker {
            tweakerClass = "com.kaolinmc.core.main.MainPartitionTweaker"
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
            entrypointClass = "com.kaolinmc.core.main.MainGradleEntrypoint"
            dependencies {
                implementation(toolingApi())
                implementation(gradleApi())
                implementation(boot())
                implementation(gradlePluginApi())
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
            url = uri("https://repo.kaolinmc.com")
            credentials {
                password = properties["creds.ext.key"] as? String
            }
        }
    }
}