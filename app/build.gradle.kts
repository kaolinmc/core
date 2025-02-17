import dev.extframework.gradle.common.*
import dev.extframework.gradle.common.dm.artifactResolver
import dev.extframework.gradle.common.dm.jobs
import util.basicExtensionInfo

version = "1.0-BETA"

repositories {
    mavenCentral()
}

dependencies {
    toolingApi()
    implementation(project(":app:app-api"))
    boot()
    jobs()
    artifactResolver()
    archives()
    commonUtil()
    objectContainer()
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

basicExtensionInfo(
    "dev.extframework.core.app.AppTweaker",
    "Application API",
    "An API for targeting applications"
)