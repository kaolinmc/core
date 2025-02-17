import dev.extframework.gradle.common.extFramework

plugins {
    kotlin("jvm") version "2.0.21"
    id("dev.extframework.common") version "1.0.49"
}

group = "dev.extframework.extension"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "dev.extframework.common")

    group = "dev.extframework.core"

    repositories {
        mavenCentral()
        extFramework()
        mavenLocal()
    }

    kotlin {
        explicitApi()
        jvmToolchain(8)
    }
}