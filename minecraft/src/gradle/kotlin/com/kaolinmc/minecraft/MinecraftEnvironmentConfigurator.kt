package com.kaolinmc.minecraft

import com.fasterxml.jackson.module.kotlin.readValue
import com.kaolinmc.archives.ArchiveReference
import com.kaolinmc.archives.Archives
import com.kaolinmc.archives.zip.ZipFinder
import com.kaolinmc.boot.monad.toList
import com.kaolinmc.boot.util.basicObjectMapper
import com.kaolinmc.common.util.make
import com.kaolinmc.common.util.resolve
import com.kaolinmc.core.app.TargetDescriptor
import com.kaolinmc.core.app.api.ApplicationTarget
import com.kaolinmc.core.instrument.InstrumentedApplicationTarget
import com.kaolinmc.core.minecraft.MinecraftApp
import com.kaolinmc.core.minecraft.util.emptyArchiveReference
import com.kaolinmc.core.minecraft.util.write
import com.kaolinmc.kiln.api.BuildEnvironment
import com.kaolinmc.kiln.api.GradleEntrypoint
import com.kaolinmc.kiln.api.descriptor
import com.kaolinmc.kiln.api.source.SourcesManager
import com.kaolinmc.tooling.api.ExtensionLoader
import com.kaolinmc.tooling.api.environment.ValueAttribute
import com.kaolinmc.tooling.api.extension.partition.artifact.PartitionArtifactRequest
import com.kaolinmc.tooling.api.extension.partition.artifact.partition
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

public class MinecraftEnvironmentConfigurator(
    private val minecraftBuildPathAttrKey: ValueAttribute.Key<Path>,
    private val minecraftUnawareAttrKey: ValueAttribute.Key<Unit>,
    private val minecraftAwareAttrKey: ValueAttribute.Key<Unit>
) {
    public suspend fun configure(
        environment: BuildEnvironment,
        helper: GradleEntrypoint.Helper
    ) {
//        environment[ExtensionLoader].tweak(
//            environment.extension.build.parents,
//            environment
//        )

        if (environment.contains(minecraftAwareAttrKey)) {
            configureAwareEnvironment(environment, helper)
        } else if (environment.contains(minecraftUnawareAttrKey)) {
            configureUnawareEnvironment(environment, helper)
        }
    }

    private suspend fun configureUnawareEnvironment(
        environment: BuildEnvironment,
        helper: GradleEntrypoint.Helper
    ) {
        // Setting up minecraft partitions
        val minecraftPartitions = environment.extension.partitions
            .filterIsInstance<MinecraftPartitionHandler>()
            .filter {
                it.dependencies.minecraftVersion == null
            }

        val loader = environment[ExtensionLoader]

        for (handler in minecraftPartitions) {
            val partitionArtifactRequest = PartitionArtifactRequest(
                environment.extension.model.descriptor.partition(
                    handler.name,
                )
            )
            val dependencies = loader.graph.cache(
                partitionArtifactRequest,
                helper.repository,
                loader.extensionResolver.partitionResolver
            )
                .parents
                .flatMap { it.toList() }

            helper.attachDependencies(
                handler,
                dependencies
            )

            // ------ Sources ------
            val sources = environment[SourcesManager]
            sources.graph.cache(
                partitionArtifactRequest,
                helper.repository,
                sources.partitionResolver
            ).parents.flatMap { it.toList() }
        }
    }

    private fun configureAwareEnvironment(
        environment: BuildEnvironment,
        helper: GradleEntrypoint.Helper
    ) {
        // Minecraft transformation / fingerprinting
        val minecraftBuildPath = environment[minecraftBuildPathAttrKey].value
        val minecraftFingerprintPath =
            minecraftBuildPath.parent resolve (minecraftBuildPath.fileName.toString() + ".fingerprint.json")

        val instrumentedApp =
            environment.find(ApplicationTarget) as? InstrumentedApplicationTarget ?: return
        val minecraftApp = instrumentedApp.delegate as? MinecraftApp ?: return

        environment.extension.finalizedBy {
            runBlocking {
                minecraftApp.setup()
                val minecraftFingerprint =
                    if (minecraftFingerprintPath.exists()) basicObjectMapper.readValue<Set<String>>(
                        minecraftFingerprintPath.toFile()
                    ) else setOf()

                val thisFingerprint = environment.extension.build.fingerprint.toSet()

                if (minecraftFingerprint != thisFingerprint) {
                    writeMinecraft(
                        minecraftBuildPath,
                        instrumentedApp,
                        minecraftApp.classpath + listOf(minecraftApp.gameJar)
                    )

                    minecraftFingerprintPath.make()
                    minecraftFingerprintPath.writeBytes(
                        basicObjectMapper.writeValueAsBytes(thisFingerprint)
                    )
                }

                // Setting up minecraft partitions
                val minecraftVersion = minecraftApp.version
                val minecraftPartitions = environment.extension.partitions
                    .filterIsInstance<MinecraftPartitionHandler>()
                    .filter {
                        it.supportedVersions.contains(minecraftVersion)
                    }

                val loader = environment[ExtensionLoader]

                for (handler in minecraftPartitions) {
                    val partitionRequest = PartitionArtifactRequest(
                        environment.extension.model.descriptor.partition(
                            handler.name,
                        )
                    )
                    val dependencies = loader.graph.cache(
                        partitionRequest,
                        helper.repository,
                        loader.extensionResolver.partitionResolver
                    )
                        .parents
                        .flatMap { it.toList() }

                    if (dependencies.any { it.value.descriptor is TargetDescriptor }) {
                        handler.dependencies.add(
                            "implementation",
                            environment.extension.project.files(
                                minecraftBuildPath.toFile()
                            )
                        )
                    }

                    helper.attachDependencies(
                        handler,
                        dependencies
                    )

                    // ------ Sources ------
                    val sources = environment[SourcesManager]
                    sources.graph.cache(
                        partitionRequest,
                        helper.repository,
                        sources.partitionResolver
                    ).parents.flatMap { it.toList() }
                }
            }
        }
    }

    private fun writeMinecraft(
        binaryPath: Path,
        // TODO sourcesPath: Path,
        app: ApplicationTarget,
        classpath: List<Path>
    ) {
        val cp = classpath
            .map { Archives.find(it, ZipFinder) }
            .flatMap { it.reader.entries() }

        val target = emptyArchiveReference()

        for (entry in cp) {
            target.writer.put(
                ArchiveReference.Entry(
                    entry.name,
                    entry.isDirectory,
                    target,
                ) {
                    // Transforming
                    app.node.handle!!.getResource(
                        entry.name
                    ) ?: entry.open()
                }
            )
        }

        binaryPath.make()
        target.write(binaryPath)
    }

}