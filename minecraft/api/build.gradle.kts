import com.kaolinmc.gradle.common.*

plugins {
    kotlin("jvm")
    `maven-publish`
}

version = "1.0.3-SNAPSHOT"

dependencies {
    implementation(project(":app:app-api"))
    implementation(project(":capability"))
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
        repositories {
            kaolin(credentials = propertyCredentialProvider)
        }

        publication {
            withJava()
            withSources()
            withDokka()

            artifactId = "minecraft-api"

            commonPom {
                packaging = "jar"

                withKaolinRepo()
                defaultDevelopers()
                gnuLicense()
                kaolinScm("ext-loader")
            }
        }
    }
}