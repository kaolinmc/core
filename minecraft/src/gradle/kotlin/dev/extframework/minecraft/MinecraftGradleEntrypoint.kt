package dev.extframework.minecraft

import com.durganmcbroom.artifact.resolver.ArtifactMetadata
import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import dev.extframework.archives.ArchiveHandle
import dev.extframework.archives.zip.ZipFinder
import dev.extframework.archives.zip.classLoaderToArchive
import dev.extframework.boot.archive.ArchiveAccessTree
import dev.extframework.boot.archive.ArchiveTarget
import dev.extframework.boot.archive.ClassLoadedArchiveNode
import dev.extframework.boot.loader.*
import dev.extframework.common.util.make
import dev.extframework.common.util.resolve
import dev.extframework.core.app.api.ApplicationDescriptor
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.minecraft.api.MappingNamespace
import dev.extframework.core.minecraft.api.MinecraftAppApi
import dev.extframework.core.minecraft.environment.mappingTargetAttrKey
import dev.extframework.core.minecraft.util.emptyArchiveHandle
import dev.extframework.core.minecraft.util.emptyArchiveReference
import dev.extframework.core.minecraft.util.write
import dev.extframework.gradle.api.*
import dev.extframework.minecraft.launch.getMinecraftDir
import dev.extframework.minecraft.launch.setupMinecraft
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.environment.ValueAttribute
import kotlinx.coroutines.runBlocking
import org.gradle.api.Project
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

public class MinecraftGradleEntrypoint : GradleEntrypoint {
    private val minecraftBuildPathAttrKey = ValueAttribute.Key<Path>("minecraft-build-path")
    private val minecraftUnawareAttrKey = ValueAttribute.Key<Unit>("minecraft-unaware")
    private val minecraftAwareAttrKey = ValueAttribute.Key<Unit>("minecraft-aware")

    override fun apply(project: Project) {}

    override fun tweak(root: BuildEnvironment): Job<Unit> = job {
        root[environmentEmitters].add(object : EnvironmentEmitter {
            override fun emit(
                extension: ExtframeworkExtension
            ): Job<List<ExtensionEnvironment>> = job {
                val neededEnvironments = extension.partitions
                    .filterIsInstance<MinecraftPartitionHandler>()
                    .mapNotNullTo(HashSet()) {
                        it.dependencies.minecraftVersion
                            ?.let { v -> v to it.mappings }
                    }

                val root by extension::defaultEnvironment

                neededEnvironments.map {
                    root.compose("Minecraft ${it.first} mapped to ${it.second.identifier}") to it
                }.onEach { (env, metadata) ->
                    val (version, mappings) = metadata
                    setupAware(env, version, mappings, extension)().merge()
                }.map { it.first } + root.compose("Minecraft unaware").also {
                    setupUnaware(it, extension.worker.dataDir resolve "minecraft")
                }
            }
        })

        root[environmentConfigurators].add(
            MinecraftEnvironmentConfigurator(
                minecraftBuildPathAttrKey,
                minecraftUnawareAttrKey,
                minecraftAwareAttrKey,
            )
        )
    }

    private fun setupUnaware(
        environment: ExtensionEnvironment,
        minecraftPath: Path
    ) {
        // Marker attribute
        environment += ValueAttribute(Unit, minecraftUnawareAttrKey)

        environment += EmptyApp(
            minecraftPath resolve ".unaware"
        )
    }

    private fun setupAware(
        environment: ExtensionEnvironment,
        version: String,
        mappings: MappingNamespace,
        extension: ExtframeworkExtension
    ): Job<Unit> = job {
        runBlocking {
            val minecraftPath =
                extension.worker.dataDir resolve
                        "minecraft" resolve
                        extension.project.path.removePrefix(":").replace(":", File.separator) resolve
                        mappings.path resolve
                        "$version.jar"

            environment += ValueAttribute(minecraftPath, minecraftBuildPathAttrKey)
            // Marker attribute
            environment += ValueAttribute(Unit, minecraftAwareAttrKey)

            val metadata = setupMinecraft(
                version,
                getMinecraftDir()
            )().merge()

            environment += ValueAttribute(
                mappings,
                mappingTargetAttrKey
            )

            environment += ClasspathApp(
                listOf(metadata.clientJar) + metadata.libraries,
                metadata.version,
                getMinecraftDir(),
                metadata.clientJar,
                metadata.mainClass
            )
        }
    }

    internal class EmptyApp(
        override val gameDir: Path,
    ) : MinecraftAppApi() {
        override val gameJar: Path = gameDir resolve "game.jar"
        override val classpath: List<Path> = listOf()
        override val version: String = "not specified"
        override val mainClass: String = ""
        override val path: Path = gameDir

        init {
            gameJar.make()
            emptyArchiveReference().write(gameJar)
        }

        override val node: ClassLoadedArchiveNode<ApplicationDescriptor> =
            object : ClassLoadedArchiveNode<ApplicationDescriptor> {
                private val thisDescriptor = ApplicationDescriptor(
                    "net.minecraft",
                    "client",
                    version,
                    null
                )

                override val handle: ArchiveHandle? = emptyArchiveHandle()
                override val descriptor: ApplicationDescriptor = thisDescriptor
                override val access: ArchiveAccessTree = object : ArchiveAccessTree {
                    override val descriptor: ArtifactMetadata.Descriptor = thisDescriptor
                    override val targets: List<ArchiveTarget> = listOf()
                }
            }
    }

    internal class ClasspathApp(
        override val classpath: List<Path>,
        override val version: String,
        override val path: Path,
        override val gameJar: Path,
        override val mainClass: String
    ) : MinecraftAppApi() {
        override val gameDir: Path = path

        override val node: ClassLoadedArchiveNode<ApplicationDescriptor> =
            object : ClassLoadedArchiveNode<ApplicationDescriptor> {
                private val applicationDescriptor = ApplicationDescriptor(
                    "net.minecraft",
                    "client",
                    version,
                    null
                )
                override val descriptor: ApplicationDescriptor = applicationDescriptor

                override val access: ArchiveAccessTree = object : ArchiveAccessTree {
                    override val descriptor: ArtifactMetadata.Descriptor = applicationDescriptor
                    override val targets: List<ArchiveTarget> = listOf()
                }
                private val references = classpath.map { it ->
                    ZipFinder.find(it)
                }

                override val handle: ArchiveHandle = classLoaderToArchive(
                    MutableClassLoader(
                        name = "minecraft-loader",
                        resources = MutableResourceProvider(
                            references.mapTo(ArrayList()) { ArchiveResourceProvider(it) }
                        ),
                        sources = MutableSourceProvider(
                            references.mapTo(ArrayList()) { ArchiveSourceProvider(it) }
                        ),
                        parent = ClassLoader.getSystemClassLoader(),
                    )
                )
            }
    }
}