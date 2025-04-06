import dev.extframework.gradle.common.archives
import dev.extframework.gradle.common.boot
import dev.extframework.gradle.common.commonUtil
import dev.extframework.gradle.common.dm.artifactResolver
import dev.extframework.gradle.common.dm.jobs
import dev.extframework.gradle.common.objectContainer
import dev.extframework.gradle.common.toolingApi

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
                toolingApi(version = "1.0.8-SNAPSHOT")

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

//dependencies {
//    implementation(project(":app"))
//
//    boot()
//    jobs()
//    artifactResolver()
//    archives()
//    commonUtil()
//    objectContainer()
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
//
//}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    explicitApi()
    jvmToolchain(8)
}