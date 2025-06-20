import dev.extframework.gradle.common.*
import dev.extframework.gradle.publish.ExtensionPublication

plugins {
    id("dev.extframework")
}

version = "1.0.3-BETA"

repositories {
    mavenLocal()
    mavenCentral()
    extFramework()
}

extension {
    model {
        attribute("unloadable", false)
    }

    partitions {
        tweaker {
            tweakerClass = "dev.extframework.core.app.AppTweaker"
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
                entrypointClass = "dev.extframework.core.app.AppGradleEntrypoint"
                dependencies {
                    implementation(gradleApi())
                    implementation(toolingApi())
                    implementation("dev.extframework:gradle-api:1.1-BETA")
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
            url = uri("https://repo.extframework.dev")
            credentials {
                password = properties["creds.ext.key"] as? String
            }
        }
    }
}