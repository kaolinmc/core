import com.kaolinmc.gradle.common.*

plugins {
    `kotlin-dsl`
    id("com.kaolinmc.common") version "0.1"
    publishing
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    kaolin()
    mavenLocal()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
    implementation(resourceApi())
    implementation( commonUtil())
            implementation( archives())
}

common {
    defaultJavaSettings()
}



