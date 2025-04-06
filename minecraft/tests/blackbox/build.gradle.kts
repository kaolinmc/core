import dev.extframework.core.main.main
import dev.extframework.gradle.common.extFramework
import dev.extframework.gradle.common.*
import dev.extframework.minecraft.minecraft
import dev.extframework.minecraft.task.GenerateMinecraftSource
import dev.extframework.minecraft.task.LaunchMinecraft
import dev.extframework.minecraft.MojangNamespaces

plugins {
    kotlin("jvm") version "2.0.21"
    id("maven-publish")
    id("dev.extframework") version "1.3.0"
    id("dev.extframework.common") version "1.0.52"
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

val launchLatest by tasks.registering(LaunchMinecraft::class) {
    dependsOn(publishToMavenLocal)
    mcVersion.set("1.21.4")
    targetNamespace = MojangNamespaces.deobfuscated.identifier
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}

val setup by tasks.registering {
    dependsOn(tasks.withType<GenerateMinecraftSource>())
}

extension {
    partitions {
        main {
            extensionClass = "com.example.BlackboxExtension"
            dependencies {
                implementation("dev.extframework.core:entrypoint:1.0-BETA")
                implementation("dev.extframework.core:capability:1.0.1-BETA")
                implementation("dev.extframework.core:minecraft-api:1.0-BETA")
            }
        }
        minecraft("target1") {
            entrypoint = "com.example.TargetEntrypoint"
            mappings = MojangNamespaces.deobfuscated
            supportVersions("1.21.4")
            dependencies {
                implementation("dev.extframework.core:capability:1.0.1-BETA")
                implementation("dev.extframework.core:entrypoint:1.0-BETA")
                mixin()
                minecraft("1.21.4")
            }
        }
        minecraft("target2") {
            entrypoint = "com.example.TargetEntrypoint2"
            mappings = MojangNamespaces.deobfuscated
            supportVersions()
            dependencies {
                implementation("dev.extframework.core:minecraft-api:1.0-BETA")
                implementation("dev.extframework.core:capability:1.0.1-BETA")
                implementation("dev.extframework.core:entrypoint:1.0-BETA")
            }
        }
    }
}