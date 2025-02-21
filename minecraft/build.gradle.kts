import dev.extframework.gradle.common.archiveMapper
import dev.extframework.gradle.common.archives
import dev.extframework.gradle.common.boot
import dev.extframework.gradle.common.commonUtil
import dev.extframework.gradle.common.dm.artifactResolver
import dev.extframework.gradle.common.dm.jobs
import dev.extframework.gradle.common.extLoader
import dev.extframework.gradle.common.launcherMetaHandler
import dev.extframework.gradle.common.mixin
import dev.extframework.gradle.common.objectContainer
import dev.extframework.gradle.common.toolingApi
import util.basicExtensionInfo

group = "dev.extframework.core"
version = "1.0.1-BETA"

dependencies {
    implementation(project(":entrypoint"))
    implementation(project(":main"))
    implementation(project(":instrument"))
    implementation(project("minecraft-api"))
    implementation(project(":app:app-api"))
    implementation(project(":app"))
    toolingApi()
    launcherMetaHandler()
    boot()
    jobs()
    artifactResolver()
    archives()
    archiveMapper(transform = true, proguard = true)
    commonUtil()
    objectContainer()
    testImplementation(kotlin("test"))
    mixin()

    extLoader(configurationName = "testImplementation")
}

kotlin {
    jvmToolchain(8)
}

basicExtensionInfo(
    "dev.extframework.core.minecraft.MinecraftTweaker",
    "Minecraft extension",
    "An extension adding support for Minecraft"
) {
    add(project(":main"))
    add(project(":instrument"))
    add(project(":app"))
}

val listDependencies by tasks.registering(ListAllDependencies::class) {
    output.set(project.layout.buildDirectory.file("resources/test/dependencies.txt"))
}

tasks.test {
    useJUnitPlatform()

    dependsOn(listDependencies)
}