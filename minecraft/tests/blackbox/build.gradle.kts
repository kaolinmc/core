import com.kaolinmc.core.main.main
import com.kaolinmc.gradle.common.kaolin
import com.kaolinmc.minecraft.MojangNamespaces
import com.kaolinmc.minecraft.minecraft
import com.kaolinmc.minecraft.task.LaunchMinecraft

plugins {
    kotlin("jvm") version "2.0.21"
    id("maven-publish")
    id("kaolin.kiln") version "0.1"
    id("com.kaolinmc.common") version "0.1"
}

group = "com.kaolinmc.extension"
version = "1.0-BETA"

repositories {
    mavenLocal()
    mavenCentral()
    kaolin()
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