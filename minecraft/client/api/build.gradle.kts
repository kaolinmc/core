import com.kaolinmc.gradle.common.*

plugins {
    kotlin("jvm")
}

group = "com.kaolinmc.core"
version = "1.0.1-SNAPSHOT"

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

                withKaolinRepo()
                defaultDevelopers()
                gnuLicense()
                kaolinScm("core")
            }

            artifactId = "minecraft-client-api"
        }
        repositories {
            kaolin(credentials = propertyCredentialProvider, type = RepositoryType.SNAPSHOTS)
        }
    }
}