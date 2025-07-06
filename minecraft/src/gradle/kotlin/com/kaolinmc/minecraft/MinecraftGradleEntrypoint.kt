package com.kaolinmc.minecraft

import com.durganmcbroom.artifact.resolver.ArtifactMetadata
import com.kaolinmc.archives.ArchiveHandle
import com.kaolinmc.archives.zip.ZipFinder
import com.kaolinmc.archives.zip.classLoaderToArchive
import com.kaolinmc.boot.archive.ArchiveAccessTree
import com.kaolinmc.boot.archive.ArchiveTarget
import com.kaolinmc.boot.archive.ClassLoadedArchiveNode
import com.kaolinmc.boot.getLogger
import com.kaolinmc.boot.loader.*
import com.kaolinmc.common.util.make
import com.kaolinmc.common.util.resolve
import com.kaolinmc.core.app.api.ApplicationDescriptor
import com.kaolinmc.core.minecraft.api.MappingNamespace
import com.kaolinmc.core.minecraft.api.MinecraftAppApi
import com.kaolinmc.core.minecraft.environment.mappingTargetAttrKey
import com.kaolinmc.core.minecraft.util.emptyArchiveHandle
import com.kaolinmc.core.minecraft.util.emptyArchiveReference
import com.kaolinmc.core.minecraft.util.write
import com.kaolinmc.kiln.api.BuildEnvironment
import com.kaolinmc.kiln.api.KaolinExtension
import com.kaolinmc.kiln.api.GradleEntrypoint
import com.kaolinmc.minecraft.launch.getMinecraftDir
import com.kaolinmc.minecraft.launch.setupMinecraft
import com.kaolinmc.minecraft.task.ReloadExtension
import com.kaolinmc.tooling.api.ExtensionLoader
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment
import com.kaolinmc.tooling.api.environment.ValueAttribute
import kotlinx.coroutines.runBlocking
import org.gradle.api.Project
import java.io.File
import java.nio.file.Path

public class MinecraftGradleEntrypoint : GradleEntrypoint {
    public companion object {
        public val minecraftBuildPathAttrKey: ValueAttribute.Key<Path> =
            ValueAttribute.Key<Path>("minecraft-build-path")
        public val minecraftUnawareAttrKey: ValueAttribute.Key<Unit> = ValueAttribute.Key<Unit>("minecraft-unaware")
        public val minecraftAwareAttrKey: ValueAttribute.Key<Unit> = ValueAttribute.Key<Unit>("minecraft-aware")
    }

    private val logger = getLogger()

    override suspend fun configure(
        extension: KaolinExtension,
        helper: GradleEntrypoint.Helper
    ) {
        registerReloadTask(extension.project)

        val neededEnvironments = extension.partitions
            .filterIsInstance<MinecraftPartitionHandler>()
            .mapNotNullTo(HashSet()) {
                it.dependencies.minecraftVersion
                    ?.let { v -> v to it.mappings }
            }

        val root by extension::rootEnvironment

        val minecraft = root.compose("Minecraft layer")

        val configurator = MinecraftEnvironmentConfigurator(
            minecraftBuildPathAttrKey,
            minecraftUnawareAttrKey,
            minecraftAwareAttrKey,
        )

        val targetEnvironments = (neededEnvironments.map {
            minecraft.compose("Minecraft ${it.first} mapped to ${it.second.identifier}") to it
        }.onEach { (env, metadata) ->
            val (version, mappings) = metadata
            setupAware(env, version, mappings, extension)
        }.map { it.first } + root.compose("Minecraft unaware").also {
            setupUnaware(it, extension.worker.dataDir resolve "minecraft")
        }).map {
            BuildEnvironment(it, extension)
        }

        for (environment in targetEnvironments) {
            // TODO decide what type of tweaker composition we want. Technically it is more
            //  "correct" to only apply to the parents, however realistically any extension
            //  depending on this one are running in a Minecraft environment and so would benefit
            //  from not having to declare a gradle partition just to self tweak (also then cannot run
            //  their tweakers before the app is setup, this is the larger issue).
            //  :
              helper.tweak(environment)
//            environment[ExtensionLoader].tweak(
//                extension.build.parents.map { it.node },
//                environment,
//            )

            configurator.configure(
                environment,
                helper
            )
        }
        extension.environments += targetEnvironments
    }

    private fun setupUnaware(
        environment: ExtensionEnvironment,
        minecraftPath: Path
    ) {
        // Marker attribute
        environment += ValueAttribute(minecraftUnawareAttrKey, Unit)

        environment += EmptyApp(
            minecraftPath resolve ".unaware"
        )
    }

    private fun setupAware(
        environment: ExtensionEnvironment,
        version: String,
        mappings: MappingNamespace,
        extension: KaolinExtension
    ) {
        runBlocking {
            val minecraftPath =
                extension.worker.dataDir resolve
                        "minecraft" resolve
                        extension.project.path.removePrefix(":").replace(":", File.separator) resolve
                        mappings.path resolve
                        "$version.jar"

            environment += ValueAttribute(minecraftBuildPathAttrKey, minecraftPath)
            // Marker attribute
            environment += ValueAttribute(minecraftAwareAttrKey, Unit)

            val metadata = setupMinecraft(
                version,
                getMinecraftDir(),
                logger
            )

            environment += ValueAttribute(
                mappingTargetAttrKey,
                mappings
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

    private fun registerReloadTask(
        project: Project
    ) {
        project.tasks.register("reload", ReloadExtension::class.java) {
            it.outputs.upToDateWhen { false }
        }
    }

    public class EmptyApp(
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

    public class ClasspathApp(
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