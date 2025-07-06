package com.kaolinmc.core.main

import com.kaolinmc.boot.archive.ArchiveGraph
import com.kaolinmc.tooling.api.exception.StructuredException
import com.kaolinmc.tooling.api.extension.ExtensionNode
import com.kaolinmc.tooling.api.extension.ExtensionResolver
import com.kaolinmc.tooling.api.extension.artifact.ExtensionDescriptor
import com.kaolinmc.tooling.api.extension.descriptor
import com.kaolinmc.tooling.api.extension.partition.ExtensionPartitionContainer
import com.kaolinmc.tooling.api.extension.partition.artifact.PartitionArtifactRequest
import com.kaolinmc.tooling.api.extension.partition.artifact.partition
import com.kaolinmc.tooling.api.uber.*

public class MainInit(
//    public val delegate: ExtensionInitializer?,
    private val extResolver: ExtensionResolver,
    private val graph: ArchiveGraph,
) {
    private val initialized = ArrayList<ExtensionDescriptor>()
    // A map of our subsystems. Any or Object is included additionally as a reference to the default.

    public suspend fun init(nodes: List<ExtensionNode>) {
        val mainDescriptor = UberDescriptor("Main partitions")
        val request = UberArtifactRequest(
            mainDescriptor,
            nodes
                .filter { it.runtimeModel.namedPartitions.contains("main") }
                .map { node ->
                    val request = PartitionArtifactRequest(node.descriptor, "main")

                    UberParentRequest(
                        request,
                        extResolver.accessBridge.repositoryFor(node.descriptor),
                        extResolver.partitionResolver,
                    )
                }
        )

        graph.cache(
            request,
            UberRepositorySettings,
            UberResolver
        )

        graph.get(
            mainDescriptor,
            UberResolver
        )

        runMainInit(nodes)
    }

    private fun runMainInit(nodes: List<ExtensionNode>) {
        nodes
            .filter { initialized.add(it.descriptor) }
            .forEach { node ->
                // Run init on main partitions
                try {
                    val mainPartition = graph.nodes[
                        node.descriptor.partition("main")
                    ]?.value as? ExtensionPartitionContainer<MainPartitionNode, MainPartitionMetadata>

                    mainPartition?.node?.entrypoint?.init()
                } catch (e: Exception) {
                    throw StructuredException(
                        ExtensionInitialization,
                        cause = e,
                        description = "Exception initializing extension"
                    ) {
                        node.runtimeModel.descriptor.name asContext "Extension name"
                    }
                }

            }
    }
}