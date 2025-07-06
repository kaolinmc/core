import com.kaolinmc.gradle.common.*

plugins {
    kotlin("jvm")
    id("com.kaolinmc.common")
}

version = "1.0.2-SNAPSHOT"

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
            kaolin(credentials = propertyCredentialProvider)
        }

        publication {
            withJava()
            withSources()
            withDokka()

            commonPom {
                packaging = "jar"

                withKaolinRepo()
                defaultDevelopers()
                gnuLicense()
                kaolinScm("mixins")
            }
        }
    }
}