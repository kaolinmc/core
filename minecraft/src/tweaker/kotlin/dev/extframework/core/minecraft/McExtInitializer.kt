package dev.extframework.core.minecraft

import dev.extframework.boot.archive.ArchiveGraph
import dev.extframework.boot.archive.ClassLoadedArchiveNode
import dev.extframework.boot.loader.ArchiveClassProvider
import dev.extframework.boot.loader.ArchiveResourceProvider
import dev.extframework.boot.loader.DelegatingClassProvider
import dev.extframework.boot.loader.DelegatingResourceProvider
import dev.extframework.core.app.TargetLinker
import dev.extframework.core.instrument.InstrumentAgent
import dev.extframework.core.main.ExtensionInitialization
import dev.extframework.core.main.MainInit
import dev.extframework.core.minecraft.mixin.MixinProcessContext
import dev.extframework.core.minecraft.mixin.MixinSubsystem
import dev.extframework.core.minecraft.partition.MinecraftPartitionLoader
import dev.extframework.core.minecraft.partition.MinecraftPartitionNode
import dev.extframework.minecraft.client.api.MinecraftExtensionInitializer
import dev.extframework.tooling.api.environment.MutableListAttribute
import dev.extframework.tooling.api.exception.StructuredException
import dev.extframework.tooling.api.extension.ExtensionNode
import dev.extframework.tooling.api.extension.ExtensionResolver
import dev.extframework.tooling.api.extension.descriptor
import dev.extframework.tooling.api.extension.partition.ExtensionPartitionContainer
import dev.extframework.tooling.api.extension.partition.artifact.PartitionArtifactRequest
import dev.extframework.tooling.api.extension.partition.artifact.partition
import dev.extframework.tooling.api.uber.*

public class McExtInitializer(
    private val instrumentationAgents: MutableListAttribute<InstrumentAgent>,
    private val linker: TargetLinker,
    public val delegate: MinecraftExtensionInitializer?,
    private val app: MinecraftApp,
    private val extResolver: ExtensionResolver,
    private val graph: ArchiveGraph,
    private val environment: String
) : MinecraftExtensionInitializer {
    override suspend fun initialize(nodes: List<ExtensionNode>) {
        app.setup()

        for (node in nodes) {
            val versions = node.runtimeModel.attributes["supportedVersions"]

            if (versions == null || versions == "*" || versions.isEmpty()) {
                continue
            } else {
                val parsedVersions = versions.split(",")

                if (!parsedVersions.contains(app.version)) {
                    throw StructuredException(
                        MinecraftException.ExtensionDoesNotSupportThisVersion,
                        null,
                        description = "This extension does not support the current minecraft version"
                    ) {
                        node.descriptor asContext "Problematic Extension"
                        app.version asContext "Current Minecraft version"
                    }
                }
            }
        }

        val mcDescriptor = UberDescriptor("Minecraft partitions")
        val request = UberArtifactRequest(
            mcDescriptor,
            nodes.flatMap { node ->
                node.runtimeModel.partitions
                    .filter { model -> model.type == MinecraftPartitionLoader.TYPE }
                    .filter { model ->
                        val enabled = (model.options["versions"]?.split(",") ?: listOf()).contains(
                            app.version
                        )

                        enabled
                    }
                    .map { model ->
                        UberParentRequest(
                            PartitionArtifactRequest(node.descriptor, model.name),
                            extResolver.accessBridge.repositoryFor(node.descriptor),
                            extResolver.partitionResolver,
                        )
                    }
            }
        )

        graph.cache(
            request,
            UberRepositorySettings,
            UberResolver
        )

        graph.get(
            mcDescriptor,
            UberResolver
        )

        for (node in nodes) {
            // TODO all partitions?
            val partitions = node.runtimeModel.partitions
                .map { node.descriptor.partition(it.name) }
                .mapNotNull { graph.nodes[it]?.value }
                .filterIsInstance<ExtensionPartitionContainer<*, *>>()

            instrumentationAgents
                .filterIsInstance<MixinSubsystem>()
                .onEach {
                    it.register(MixinProcessContext(node, partitions))
                }
                .forEach {
                    it.runPreprocessors()
                }

            // TODO this creates duplicates if two extensions rely on the same library.
            linker.addExtensionClasses(
                DelegatingClassProvider(
                    partitions
                        .flatMap { it.access.targets.map { it.relationship.node } + it }
                        .filterIsInstance<ClassLoadedArchiveNode<*>>()
                        .map { it.handle }
                        .map { ArchiveClassProvider(it) }
                )
            )
            linker.addExtensionResources(
                DelegatingResourceProvider(
                    partitions
                    .map { it.handle }
                    .map { ArchiveResourceProvider(it) }
            ))

            // Run init on target partitions
            partitions
                .forEach { container ->
                    val partNode = container.node as? MinecraftPartitionNode ?: return@forEach

                    try {
                        partNode.entrypoint?.init()
                    } catch (e: Exception) {
                        throw StructuredException(
                            ExtensionInitialization,
                            cause = e,
                            description = "Exception initializing minecraft partition"
                        ) {
                            node.runtimeModel.descriptor.name asContext "Extension name"
                            container.metadata.name asContext "Partition name"
                        }
                    }
                }

        }

        MainInit(extResolver, graph).init(nodes)
    }
}