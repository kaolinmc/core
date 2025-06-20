import dev.extframework.gradle.common.RepositoryType
import dev.extframework.gradle.common.extFramework
import dev.extframework.gradle.common.toolingApi

plugins {
    kotlin("jvm")
}

group = "dev.extframework.core"
version = "1.0.1-BETA"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
   implementation(toolingApi())
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
        publication {
            withSources()
            withDokka()
            withJava()

            commonPom {
                packaging = "jar"

                withExtFrameworkRepo()
                defaultDevelopers()
                gnuLicense()
                extFrameworkScm("core")
            }

            artifactId = "minecraft-client-api"
        }
        repositories {
            extFramework(credentials = propertyCredentialProvider, type = RepositoryType.SNAPSHOTS)
        }
    }
}