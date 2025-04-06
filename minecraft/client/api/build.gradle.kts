import dev.extframework.gradle.common.RepositoryType
import dev.extframework.gradle.common.extFramework

plugins {
    kotlin("jvm")
}

group = "dev.extframework.core"
version = "1.0-BETA"

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


common {
    defaultJavaSettings()

    publishing {
        publication {
            withSources()
            withDokka()
            withJava()

            artifactId = "minecraft-client-api"
        }
        repositories {
            extFramework(credentials = propertyCredentialProvider, type = RepositoryType.RELEASES)
        }
    }
}