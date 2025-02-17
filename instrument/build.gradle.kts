import dev.extframework.gradle.common.archives
import dev.extframework.gradle.common.boot
import dev.extframework.gradle.common.commonUtil
import dev.extframework.gradle.common.dm.artifactResolver
import dev.extframework.gradle.common.dm.jobs
import dev.extframework.gradle.common.objectContainer
import dev.extframework.gradle.common.toolingApi
import util.basicExtensionInfo

plugins {
    kotlin("jvm")
}

version = "1.0-BETA"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":app:app-api"))
    implementation(project(":app"))
    toolingApi()
    boot()
    jobs()
    artifactResolver()
    archives()
    commonUtil()
    objectContainer()
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}

basicExtensionInfo(
    "dev.extframework.core.instrument.InstrumentTweaker",
    "Instrumentation API",
    "An API for instrumenting the application target"
) {
    add(project(":app"))
}