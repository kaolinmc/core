import dev.extframework.gradle.common.archives
import dev.extframework.gradle.common.boot
import dev.extframework.gradle.common.commonUtil
import dev.extframework.gradle.common.dm.artifactResolver
import dev.extframework.gradle.common.dm.jobs
import dev.extframework.gradle.common.extFramework
import dev.extframework.gradle.common.objectContainer
import dev.extframework.gradle.common.toolingApi
import dev.extframework.gradle.publish.ExtensionPublication
import kotlin.jvm.java

plugins {
    id("dev.extframework")
}

version = "1.0.1-BETA"

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
                toolingApi()
                implementation(project("app-api"))
                boot()
                jobs()
                artifactResolver()
                archives()
                commonUtil()
                objectContainer()
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
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