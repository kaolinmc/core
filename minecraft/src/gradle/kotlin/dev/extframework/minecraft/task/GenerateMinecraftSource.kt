package dev.extframework.minecraft.task

import BootLoggerFactory
import com.durganmcbroom.jobs.launch
import dev.extframework.archive.mapper.findShortest
import dev.extframework.archive.mapper.newMappingsGraph
import dev.extframework.common.util.resolve
import dev.extframework.core.minecraft.api.MappingNamespace
import dev.extframework.core.minecraft.environment.mappingProvidersAttrKey
import dev.extframework.gradle.api.ExtensionWorker
import dev.extframework.minecraft.MojangNamespaces
import dev.extframework.minecraft.launch.getMinecraftDir
import dev.extframework.minecraft.launch.remapMinecraft
import dev.extframework.minecraft.launch.setupMinecraft
import dev.extframework.minecraft.mavenLocal
import dev.extframework.tooling.api.environment.extract
import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.io.path.name

public abstract class GenerateMinecraftSource : DefaultTask() {
    private val basePath = project.mavenLocal

    @get:Input
    public abstract val minecraftVersion: Property<String>

    @get:Input
    public abstract val namespace: Property<MappingNamespace>

    @get:OutputFile
    public val minecraftOut: RegularFileProperty
        get() = project.objects.fileProperty().convention {
            val version = minecraftVersion.get()
            (basePath resolve "net" resolve "minecraft" resolve "client" resolve version resolve namespace.get().path resolve "minecraft-${version}.jar").toFile()
        }

    @get:OutputDirectory
    public val minecraftLibsOut: ConfigurableFileCollection
        get() = project.objects.fileCollection().from(
            run {
                val version = minecraftVersion.get()
                (basePath resolve "net" resolve "minecraft" resolve "client" resolve version resolve namespace.get().path resolve "libs").toFile()
            }
        )

    @TaskAction
    public fun generateSources() {
        val extension = project.rootProject.extensions.getByType(ExtensionWorker::class.java)

        launch(BootLoggerFactory()) {
            runBlocking {
                val metadata = setupMinecraft(
                    minecraftVersion.orNull
                        ?: throw IllegalArgumentException("Minecraft version for minecraft source generation not set!"),
                    getMinecraftDir()
                )().merge()

                for (paths in metadata.libraries) {
                    val libPath =
                        basePath resolve "net" resolve "minecraft" resolve "client" resolve metadata.version resolve namespace.get().path resolve "libs"

                    val filePath = libPath resolve paths.name

                    if (!filePath.exists()) {
                        paths.copyTo(filePath)
                    }
                }

                val mappings = newMappingsGraph(extension.loader.environment[mappingProvidersAttrKey].extract())
                    .findShortest(MojangNamespaces.obfuscated.identifier, namespace.get().identifier)

                remapMinecraft(
                    metadata,
                    project.mavenLocal,
                    mappings.forIdentifier(metadata.version),
                    namespace.get()
                )
            }
        }
    }
}