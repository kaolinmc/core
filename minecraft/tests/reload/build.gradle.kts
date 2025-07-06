import com.kaolinmc.core.main.main
import com.kaolinmc.gradle.common.*
import com.kaolinmc.minecraft.minecraft
import com.kaolinmc.minecraft.task.LaunchMinecraft
import com.kaolinmc.minecraft.MojangNamespaces

plugins {
    kotlin("jvm") version "2.0.21"
    id("maven-publish")
    id("kaolin.kiln") version "0.1"
    id("com.kaolinmc.common") version "0.1"
}

group = "com.example"
version = "1"

repositories {
    mavenLocal()
    mavenCentral()
    kaolin()
}

extension {
    partitions {
        main {
            extensionClass = "com.example.Reload"
            dependencies {  }
        }
        minecraft("target") {
            supportVersions("1")
            dependencies {}
        }
        minecraft("target-reload") {
            supportVersions("2")
            dependencies {}
        }
    }
}

dependencies {
    "targetImplementation"(project("target-app"))
    "target-reloadImplementation"(project("target-app"))
}
