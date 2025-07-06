import com.kaolinmc.gradle.common.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(8)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
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