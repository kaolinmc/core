import dev.extframework.gradle.common.*
import dev.extframework.gradle.common.dm.artifactResolver
import dev.extframework.gradle.common.dm.jobs
import util.basicExtensionInfo


version = "1.0-BETA"

dependencies {
    toolingApi()
    implementation(project(":entrypoint"))
    boot()
    jobs()
    artifactResolver()
    archives()
    commonUtil()
    objectContainer()
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
}

// Bundling and publishing
basicExtensionInfo(
    "dev.extframework.core.main.MainPartitionTweaker",
    "Main partition",
    "An extension that adds a main partition for loading environment agnostic code"
)