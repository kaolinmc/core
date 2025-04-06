import dev.extframework.gradle.common.extFramework
import dev.extframework.gradle.common.toolingApi

plugins {
    kotlin("jvm")
    `maven-publish`
}

version = "1.0-BETA"

dependencies {
    implementation(project(":app:app-api"))
    implementation(project(":capability"))
    
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