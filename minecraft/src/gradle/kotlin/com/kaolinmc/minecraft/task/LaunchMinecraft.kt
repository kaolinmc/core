package com.kaolinmc.minecraft.task

import com.durganmcbroom.artifact.resolver.ArtifactMetadata
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenDescriptor
import com.kaolinmc.boot.getLogger
import com.kaolinmc.boot.util.basicObjectMapper
import com.kaolinmc.common.util.deleteAll
import com.kaolinmc.common.util.make
import com.kaolinmc.common.util.resolve
import com.kaolinmc.kiln.api.KaolinExtension
import com.kaolinmc.launchermeta.handler.DefaultMetadataProcessor
import com.kaolinmc.minecraft.MinecraftGradleEntrypoint
import com.kaolinmc.minecraft.client.api.LaunchContext
import com.kaolinmc.minecraft.launch.setupMinecraft
import com.kaolinmc.minecraft.mavenLocal
import kotlinx.coroutines.runBlocking
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.FileOutputStream
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path

public const val CLIENT_VERSION: String = "1.0-BETA"
public const val CLIENT_MAIN_CLASS: String = "com.kaolinmc.dev.client.Main"

private fun getHomedir(): Path {
    return getMinecraftDir() resolve ".kaolin"
}

private fun getMinecraftDir(): Path {
    val osName = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")

    return when {
        osName.contains("win") -> {
            val appData = System.getenv("APPDATA")?.let(::Path) ?: Path(userHome, "AppData", "Roaming")
            appData resolve ".minecraft"
        }

        osName.contains("mac") -> Paths.get(userHome, "Library", "Application Support", "minecraft")
        else -> Paths.get(userHome, ".minecraft") // Assuming Linux/Unix-like
    }
}

public fun preDownloadClient(version: String): Path {
    return getHomedir() resolve "client-$version.jar"
}

public fun downloadClient(version: String): Path {
    val outputPath = preDownloadClient(version)

    if (outputPath.make()) {
        val inputStream = MinecraftGradleEntrypoint::class.java.getResourceAsStream("/client.jar")!!

        inputStream.use { itin ->
            FileOutputStream(outputPath.toFile()).use { itout ->
                itin.copyTo(itout)
            }
        }
    }

    return outputPath
}

public fun preCacheExtension(project: Project, ext: KaolinExtension): Pair<ArtifactMetadata.Descriptor, String> {
    val repositoryDir = project.mavenLocal
    val erm = ext.model
    val descriptor = SimpleMavenDescriptor(erm.groupId.get(), erm.name.get(), erm.version.get(), null)

    return descriptor to repositoryDir.toString()
}

public abstract class LaunchMinecraft : JavaExec() {
    @Input
    @Optional
    public val mcVersion: Property<String> = project.objects.property(String::class.java)

    @get:Input
    public abstract val targetNamespace: Property<String>

    @get:Input
    public val minecraftArguments: MapProperty<String, String> =
        project.objects.mapProperty(String::class.java, String::class.java)
            .convention(mutableMapOf("auth_access_token" to ""))

    @TaskAction
    @ExperimentalPathApi
    override fun exec(): Unit = runBlocking {
        val mapper = basicObjectMapper

        val extension = project.extensions.getByType(KaolinExtension::class.java)

        val mcDir = getMinecraftDir()
        val binDir = mcDir resolve "bin"
        binDir.deleteAll()

        val env = setupMinecraft(
            mcVersion.get(),
            mcDir,
            getLogger()
        )

        val path = downloadClient(CLIENT_VERSION)
        val (desc, repo) = preCacheExtension(project, extension)

        classpath(path)
        workingDir(mcDir.toFile())

        val mcVersion = mcVersion.get()
        val extensionPath = extension.project.layout.buildDirectory.get().asFile.toPath() resolve "extension"

        val values = mapOf(
            "version" to mcVersion,
            "version_name" to mcVersion,
            "game_directory" to mcDir.toString(),
            "assets_root" to env.assets.toString(),
            "assets_index_name" to env.assetIndex,
            "natives_directory" to env.nativesDir.toString(),
            "classpath" to "~/nothing.jar"
        ) + minecraftArguments.get()

        val processor = DefaultMetadataProcessor()

        env.arguments.jvm.forEach { arg ->
            val arg = processor.formatArg(values, arg) ?: return@forEach

            jvmArgs(arg)
        }

        val args = env.arguments.game
            .chunked(2)
            .flatMap { (arg1, arg2) ->
                val first: List<String> = processor.formatArg(values, arg1) ?: return@flatMap listOf()
                val second: List<String> = processor.formatArg(values, arg2) ?: return@flatMap listOf()


                first + second
            }

        val context = LaunchContext(
            desc.name,
            repo,
            targetNamespace.get(),
            extensionPath,
            mcVersion,
            env.mainClass,
            listOf(env.clientJar) + env.libraries,
            env.clientJar,
            args
        )

        args(mapper.writeValueAsString(context))

        super.exec()
    }
}

internal fun Project.registerLaunchTask(extension: KaolinExtension, publishTask: Task) =
    tasks.register("launch", LaunchMinecraft::class.java) { exec ->

        exec.dependsOn(publishTask)
        exec.mainClass.set(CLIENT_MAIN_CLASS)
    }