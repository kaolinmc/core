import dev.extframework.gradle.common.boot
import dev.extframework.gradle.common.extFramework
import dev.extframework.gradle.common.toolingApi

plugins {
    kotlin("jvm")
    id("dev.extframework.common")
}

version = "1.0.2-BETA"

dependencies {
    implementation(boot())
//    implementation(artifactResolver(maven = true))
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