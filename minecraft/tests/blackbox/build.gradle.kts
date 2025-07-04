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

group = "dev.extframework.extension"
version = "1.0-BETA"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://repo.extframework.dev/registry")
    }
    extFramework()
}

val publishToMavenLocal by tasks.getting

val launch by tasks.registering(LaunchMinecraft::class) {
    mcVersion.set("1.21.4")
    targetNamespace.set(MojangNamespaces.deobfuscated.identifier)
    dependsOn(tasks.named("publishToMavenLocal"))
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}

//val setup by tasks.registering {
//    dependsOn(tasks.withType<GenerateMinecraftSource>())
//}

kotlin {
    jvmToolchain(21)
}

extension {
    partitions {
        main {
            extensionClass = "com.example.BlackboxExtension"
            dependencies {
            }
        }
        minecraft("target1") {
            entrypoint = "com.example.TargetEntrypoint"
            mappings = MojangNamespaces.deobfuscated
            supportVersions("1.21.4")
            dependencies {
                minecraft("1.21.4")
            }
        }

    }
}