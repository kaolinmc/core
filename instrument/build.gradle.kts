import com.kaolinmc.gradle.common.*
import com.kaolinmc.kiln.publish.*
import kotlin.jvm.java

plugins {
    id("kaolin.kiln")
}

version = "1.0.5-BETA"

repositories {
    mavenCentral()
}

extension {
    model {
        attribute("unloadable", false)
    }
    partitions {
        tweaker {
            tweakerClass = "com.kaolinmc.core.instrument.InstrumentTweaker"
            dependencies {
                implementation(project(":app:app-api"))

                implementation(toolingApi())
                implementation(boot())
                implementation(artifactResolver())
                implementation(archives())
                implementation(commonUtil())
                implementation(objectContainer())

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("net.bytebuddy:byte-buddy-agent:1.17.1")

            }
        }
    }

    metadata {
        name = "Instrumentation API"
        description = "An API for instrumenting the application target"
        app = "minecraft"
    }
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    explicitApi()
    jvmToolchain(8)
}

publishing {
    publications {
        create("prod", ExtensionPublication::class.java)
    }
    repositories {
        maven {
            url = uri("https://repo.kaolinmc.com")
            credentials {
                password = properties["creds.ext.key"] as? String
            }
        }
    }
}