import com.kaolinmc.gradle.common.*

group = "com.kaolinmc.core"
version = "1.0-SNAPSHOT"

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