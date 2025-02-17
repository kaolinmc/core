import dev.extframework.gradle.common.boot
import dev.extframework.gradle.common.dm.artifactResolver
import dev.extframework.gradle.common.extFramework
import dev.extframework.gradle.common.toolingApi

plugins {
    kotlin("jvm")
}

version = "1.0-BETA"

dependencies {
    toolingApi()
    boot()
    artifactResolver(maven = true)
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
                extFrameworkScm("mixins")
            }
        }
    }
}