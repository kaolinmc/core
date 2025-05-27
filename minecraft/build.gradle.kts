import dev.extframework.gradle.common.*
import dev.extframework.gradle.common.dm.artifactResolver
import dev.extframework.gradle.common.dm.jobs

plugins {
    id("dev.extframework")
}

group = "dev.extframework.core"
version = "1.0.3-BETA"

repositories {
    mavenLocal()
}

kotlin {
    jvmToolchain(8)
    explicitApi()
}

extension {
    partitions {
        tweaker {
            tweakerClass = "dev.extframework.core.minecraft.MinecraftTweaker"
            dependencies {
                implementation(project("minecraft-api"))
                implementation(project("client:client-api"))
                implementation(project(":app:app-api"))
                implementation(project(":entrypoint"))
                launcherMetaHandler()
                boot()
                jobs()
                toolingApi()
                artifactResolver()
                archives()
                archiveMapper(transform = true, proguard = true)
                commonUtil()
                objectContainer()
                mixin(version = "1.0.2-SNAPSHOT")
            }
        }

        gradle {
            entrypointClass = "dev.extframework.minecraft.MinecraftGradleEntrypoint"
            dependencies {
                implementation(project("client:client-api"))
                implementation(project("minecraft-api"))
                implementation(project(":app:app-api"))
                implementation(gradleApi())
                boot()
                jobs()
                toolingApi()
                artifactResolver()
                implementation("dev.extframework:gradle-api:1.0.1-BETA")
                archives()
                archiveMapper(transform = true, proguard = true)
                commonUtil()
                objectContainer()
                mixin(version = "1.0.2-SNAPSHOT")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
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

tasks.named<org.gradle.jvm.tasks.Jar>("gradleJar") {
    from(project("client").tasks.named("jar")) {
        rename { "client.jar" }
    }
}