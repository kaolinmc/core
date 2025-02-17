package dev.extframework.core.main

import com.durganmcbroom.jobs.async.AsyncJob
import com.durganmcbroom.jobs.async.asyncJob
import dev.extframework.boot.archive.ArchiveGraph
import dev.extframework.tooling.api.extension.ExtensionNode
import dev.extframework.tooling.api.extension.ExtensionPreInitializer
import dev.extframework.tooling.api.extension.ExtensionResolver
import dev.extframework.tooling.api.extension.partition.artifact.PartitionArtifactRequest

public class MainPreInit(
    public val graph: ArchiveGraph,
    public val extResolver: ExtensionResolver,
    private val delegate: ExtensionPreInitializer?
) : ExtensionPreInitializer {
    override fun preInit(node: ExtensionNode, actions: ExtensionPreInitializer.Actions): AsyncJob<Unit> = asyncJob {
        delegate?.preInit(node, actions)?.invoke()?.merge()

        if (node.runtimeModel.partitions.any {
                it.name == "main"
            }) {

            val request = PartitionArtifactRequest(node.descriptor, "main")

            actions.addRequest(
                request,
                extResolver.accessBridge.repositoryFor(node.descriptor),
                extResolver.partitionResolver,
            )
        }
    }
}