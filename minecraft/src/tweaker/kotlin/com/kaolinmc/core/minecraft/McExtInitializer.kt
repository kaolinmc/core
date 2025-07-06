package com.kaolinmc.core.minecraft

import com.kaolinmc.boot.archive.ArchiveGraph
import com.kaolinmc.boot.archive.ClassLoadedArchiveNode
import com.kaolinmc.boot.loader.ArchiveClassProvider
import com.kaolinmc.boot.loader.ArchiveResourceProvider
import com.kaolinmc.boot.loader.DelegatingClassProvider
import com.kaolinmc.boot.loader.DelegatingResourceProvider
import com.kaolinmc.core.app.TargetLinker
import com.kaolinmc.core.instrument.InstrumentAgent
import com.kaolinmc.core.main.ExtensionInitialization
import com.kaolinmc.core.main.MainInit
import com.kaolinmc.core.minecraft.mixin.MixinProcessContext
import com.kaolinmc.core.minecraft.mixin.MixinSubsystem
import com.kaolinmc.core.minecraft.partition.MinecraftPartitionLoader
import com.kaolinmc.core.minecraft.partition.MinecraftPartitionNode
import com.kaolinmc.minecraft.client.api.MinecraftExtensionInitializer
import com.kaolinmc.mixin.engine.tag.ClassTag
import com.kaolinmc.tooling.api.environment.MutableListAttribute
import com.kaolinmc.tooling.api.exception.StructuredException
import com.kaolinmc.tooling.api.extension.ExtensionNode
import com.kaolinmc.tooling.api.extension.ExtensionResolver
import com.kaolinmc.tooling.api.extension.artifact.ExtensionDescriptor
import com.kaolinmc.tooling.api.extension.descriptor
import com.kaolinmc.tooling.api.extension.partition.ExtensionPartitionContainer
import com.kaolinmc.tooling.api.extension.partition.artifact.PartitionArtifactRequest
import com.kaolinmc.tooling.api.extension.partition.artifact.partition
import com.kaolinmc.tooling.api.uber.*

public class McExtInitializer(
    private val instrumentationAgents: MutableListAttribute<InstrumentAgent>,
    private val linker: TargetLinker,
    public val delegate: MinecraftExtensionInitializer?,
    private val app: MinecraftApp,
    private val extResolver: ExtensionResolver,
    private val graph: ArchiveGraph,
    private val environment: String
) : MinecraftExtensionInitializer {
    internal val extensionMixins = HashMap<ExtensionDescriptor, Set<ClassTag>>()

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
                    extensionMixins[node.descriptor] = it.register(MixinProcessContext(node, partitions))
                }
                .forEach {
                    it.runPreprocessors()
                }

            linker.extensionClasses[node.descriptor] = DelegatingClassProvider(
                partitions
                    .flatMap { it.access.targets.map { it.relationship.node } + it }
                    .filterIsInstance<ClassLoadedArchiveNode<*>>()
                    .map { it.handle }
                    .map { ArchiveClassProvider(it) }
            )

            linker.extensionResources[node.descriptor] = DelegatingResourceProvider(
                partitions
                    .map { it.handle }
                    .map { ArchiveResourceProvider(it) }
            )

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