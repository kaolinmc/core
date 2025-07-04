import dev.extframework.gradle.common.*
import dev.extframework.gradle.publish.ExtensionPublication
import kotlin.jvm.java

plugins {
    id("dev.extframework")
}

group = "dev.extframework.core"
version = "1.0.7-BETA"

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
        developers = listOf("extframework")
        description = "Adds Minecraft support to the extframework ecosystem"
    }
    partitions {
        tweaker {
            tweakerClass = "dev.extframework.core.minecraft.MinecraftTweaker"
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
            entrypointClass = "dev.extframework.minecraft.MinecraftGradleEntrypoint"
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
            url = uri("https://repo.extframework.dev")
            credentials {
                password = properties["creds.ext.key"] as? String
            }
        }
    }
}