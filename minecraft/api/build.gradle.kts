import dev.extframework.gradle.common.extFramework
import dev.extframework.gradle.common.toolingApi

plugins {
    kotlin("jvm")
    `maven-publish`
}

version = "1.0.1-BETA"

dependencies {
    implementation(project(":app:app-api"))
    implementation(project(":capability"))
    toolingApi()
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

common {
    defaultJavaSettings()
    publishing {
        repositories {
            extFramework(credentials = propertyCredentialProvider)
        }

        publication {
            withJava()
            withSources()
            withDokka()

            artifactId = "minecraft-api"

            commonPom {
                packaging = "jar"

                withExtFrameworkRepo()
                defaultDevelopers()
                gnuLicense()
                extFrameworkScm("ext-loader")
            }
        }
    }
}