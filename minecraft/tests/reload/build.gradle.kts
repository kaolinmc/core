import dev.extframework.core.main.main
import dev.extframework.gradle.common.extFramework
import dev.extframework.gradle.common.*
import dev.extframework.minecraft.minecraft
import dev.extframework.minecraft.task.LaunchMinecraft
import dev.extframework.minecraft.MojangNamespaces

plugins {
    kotlin("jvm") version "2.0.21"
    id("maven-publish")
    id("dev.extframework") version "1.4.1"
    id("dev.extframework.common") version "1.1"
}

group = "com.example"
version = "1"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://repo.extframework.dev/registry")
    }
    extFramework()
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
