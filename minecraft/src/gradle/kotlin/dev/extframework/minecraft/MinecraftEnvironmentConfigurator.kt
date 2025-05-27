package dev.extframework.minecraft

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import com.durganmcbroom.jobs.logging.warning
import com.fasterxml.jackson.module.kotlin.readValue
import dev.extframework.archives.ArchiveReference
import dev.extframework.archives.Archives
import dev.extframework.archives.zip.ZipFinder
import dev.extframework.boot.monad.toList
import dev.extframework.boot.util.basicObjectMapper
import dev.extframework.common.util.make
import dev.extframework.common.util.resolve
import dev.extframework.core.app.TargetDescriptor
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.instrument.InstrumentedApplicationTarget
import dev.extframework.core.minecraft.MinecraftApp
import dev.extframework.core.minecraft.util.emptyArchiveReference
import dev.extframework.core.minecraft.util.write
import dev.extframework.gradle.api.BuildEnvironment
import dev.extframework.gradle.api.BuildEnvironmentConfigurator
import dev.extframework.gradle.api.descriptor
import dev.extframework.tooling.api.ExtensionLoader
import dev.extframework.tooling.api.environment.ValueAttribute
import dev.extframework.tooling.api.extension.partition.artifact.PartitionArtifactRequest
import dev.extframework.tooling.api.extension.partition.artifact.partition
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.writeBytes

public class MinecraftEnvironmentConfigurator(
    private val minecraftBuildPathAttrKey: ValueAttribute.Key<Path>,
    private val minecraftUnawareAttrKey: ValueAttribute.Key<Unit>,
    private val minecraftAwareAttrKey: ValueAttribute.Key<Unit>
) : BuildEnvironmentConfigurator {
    override fun configure(
        environment: BuildEnvironment,
        helper: BuildEnvironmentConfigurator.Helper
    ): Job<Unit> = job {
        if (environment.contains(minecraftAwareAttrKey)) {
            configureAwareEnvironment(environment, helper)().merge()
        } else if (environment.contains(minecraftUnawareAttrKey)) {
            configureUnawareEnvironment(environment, helper)().merge()
        }
    }

    private fun configureUnawareEnvironment(
        environment: BuildEnvironment,
        helper: BuildEnvironmentConfigurator.Helper
    ) = job {
        // Setting up minecraft partitions
        val minecraftPartitions = environment.extension.partitions
            .filterIsInstance<MinecraftPartitionHandler>()
            .filter {
                it.dependencies.minecraftVersion == null
            }

        val loader = environment[ExtensionLoader]

        for (handler in minecraftPartitions) {
            val dependencies = loader.graph.cache(
                PartitionArtifactRequest(
                    environment.extension.model.descriptor.partition(
                        handler.name,
                        loader.rootEnvironment.name
                    )
                ),
                helper.repository,
                loader.extensionResolver.partitionResolver
            )().merge()
                .parents
                .flatMap { it.toList() }

            helper.attachDependencies(
                handler,
                dependencies
            )
        }
    }

    private fun configureAwareEnvironment(
        environment: BuildEnvironment,
        helper: BuildEnvironmentConfigurator.Helper
    ) = job {
        val instrumentedApp =
            environment.find(ApplicationTarget) as? InstrumentedApplicationTarget ?: return@job
        val minecraftApp = instrumentedApp.delegate as? MinecraftApp ?: return@job

        minecraftApp.setup()().merge()

        // Minecraft transformation / fingerprinting
        val minecraftBuildPath = environment[minecraftBuildPathAttrKey].value
        val minecraftFingerprintPath =
            minecraftBuildPath.parent resolve (minecraftBuildPath.fileName.toString() + ".fingerprint.json")

        val minecraftFingerprint =
            if (minecraftFingerprintPath.exists()) basicObjectMapper.readValue<Set<String>>(
                minecraftFingerprintPath.toFile()
            ) else setOf()

        // TODO more complete fingerprint
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
            val dependencies = loader.graph.cache(
                PartitionArtifactRequest(
                    environment.extension.model.descriptor.partition(
                        handler.name,
                        loader.rootEnvironment.name
                    )
                ),
                helper.repository,
                loader.extensionResolver.partitionResolver
            )().merge()
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