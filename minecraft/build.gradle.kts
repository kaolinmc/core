import com.kaolinmc.gradle.common.*
import com.kaolinmc.kiln.publish.*
import kotlin.jvm.java

plugins {
    id("kaolin.kiln")
}

group = "com.kaolinmc.core"
version = "1.0.8-BETA"

repositories {
    mavenLocal()
}

kotlin {
    jvmToolchain(8)
    explicitApi()
}

extension {
    model {
        attribute("unloadable", false)
    }
    metadata {
        name = "Minecraft core"
        app = "minecraft"
        developers = listOf("kaolin")
        description = "Adds Minecraft support to the Kaolin ecosystem"
    }
    partitions {
        tweaker {
            tweakerClass = "com.kaolinmc.core.minecraft.MinecraftTweaker"
            dependencies {
                implementation(project("minecraft-api"))
                implementation(project("client:client-api"))
                implementation(project(":app:app-api"))
                implementation(project(":entrypoint"))

                implementation(launcherMetaHandler())
                implementation(boot())
                implementation(toolingApi())
                implementation(artifactResolver())
                implementation(archives())
                implementation(archiveMapper())
                implementation(archiveMapperProguard())
                implementation(archiveMapperTransform())
                implementation(commonUtil())
                implementation(objectContainer())
                implementation(mixin())
            }
        }

        gradle {
            entrypointClass = "com.kaolinmc.minecraft.MinecraftGradleEntrypoint"
            dependencies {
                implementation(project("client:client-api"))
                implementation(project("minecraft-api"))
                implementation(project(":app:app-api"))

                implementation(gradleApi())
                implementation(boot())
                implementation(toolingApi())
                implementation(artifactResolver())
                implementation(gradlePluginApi())
                implementation(archives())
                implementation(archiveMapper())
                implementation(archiveMapperTransform())
                implementation(archiveMapperProguard())
                implementation(commonUtil())
                implementation(objectContainer())
                implementation(mixin())

                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
            }
        }
    }
}

val listDependencies by tasks.registering(ListAllDependencies::class) {
    output.set(project.layout.buildDirectory.file("resources/test/dependencies.txt"))
}

tasks.test {
    useJUnitPlatform()

    dependsOn(listDependencies)
}

evaluationDependsOn("client")
tasks.named<org.gradle.jvm.tasks.Jar>("gradleJar") {
    from(project("client").tasks.named("shadowJar")) {
        rename { "client.jar" }
    }
}

publishing {
    publications {
        create("prod", ExtensionPublication::class.java)
    }
    repositories {
        maven {
            url = uri("http://127.0.0.1:6969")
            credentials {
                password = "a"
            }
        }
//        maven {
//            url = uri("https://repo.kaolinmc.com")
//            credentials {
//                password = properties["creds.ext.key"] as? String
//            }
//        }
    }
}