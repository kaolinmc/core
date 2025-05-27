import dev.extframework.gradle.common.archives
import dev.extframework.gradle.common.boot
import dev.extframework.gradle.common.commonUtil
import dev.extframework.gradle.common.dm.artifactResolver
import dev.extframework.gradle.common.dm.jobs
import dev.extframework.gradle.common.objectContainer
import dev.extframework.gradle.common.toolingApi
import dev.extframework.gradle.publish.ExtensionPublication
import kotlin.jvm.java

plugins {
    id("dev.extframework")
}

version = "1.0.1-BETA"

repositories {
    mavenCentral()
}

extension {
    model {
        attribute("unloadable", false)
    }
    partitions {
        tweaker {
            tweakerClass = "dev.extframework.core.instrument.InstrumentTweaker"
            dependencies {
                toolingApi()

                implementation(project(":app:app-api"))
                boot()
                jobs()
                artifactResolver()
                archives()
                commonUtil()
                objectContainer()
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("net.bytebuddy:byte-buddy-agent:1.17.1")

            }
        }
    }

    metadata {
        name = "Instrumentation API"
        description = "An API for instrumenting the application target"
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    explicitApi()
    jvmToolchain(8)
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