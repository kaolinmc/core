import com.kaolinmc.gradle.common.*
import com.kaolinmc.kiln.publish.ExtensionPublication

plugins {
    id("kaolin.kiln")
}

version = "1.0.5-BETA"

repositories {
    mavenLocal()
    mavenCentral()
    kaolin()
}

extension {
    model {
        attribute("unloadable", false)
    }

    partitions {
        tweaker {
            tweakerClass = "com.kaolinmc.core.app.AppTweaker"
            dependencies {
                implementation(project("app-api"))

                implementation(toolingApi())
                implementation(boot())
                implementation(artifactResolver())
                implementation(archives())
                implementation(commonUtil())
                implementation(objectContainer())
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
            }
            gradle {
                entrypointClass = "com.kaolinmc.core.app.AppGradleEntrypoint"
                dependencies {
                    implementation(boot())
                    implementation(objectContainer())
                    implementation(gradleApi())
                    implementation(toolingApi())
                    implementation(gradlePluginApi())
                }
            }
        }
    }

    metadata {
        name = "Application API"
        description = "An API for targeting applications"
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