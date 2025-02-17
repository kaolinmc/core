package dev.extframework.core.minecraft

import com.durganmcbroom.jobs.async.AsyncJob
import com.durganmcbroom.jobs.async.asyncJob
import dev.extframework.core.minecraft.partition.MinecraftPartitionLoader
import dev.extframework.tooling.api.extension.ExtensionNode
import dev.extframework.tooling.api.extension.ExtensionPreInitializer
import dev.extframework.tooling.api.extension.ExtensionResolver
import dev.extframework.tooling.api.extension.partition.artifact.PartitionArtifactRequest

internal class MinecraftPreInitializer(
    val app: MinecraftApp,
    val extResolver: ExtensionResolver,
    val delegate: ExtensionPreInitializer?,
) : ExtensionPreInitializer {
    override fun preInit(
        node: ExtensionNode,
        actions: ExtensionPreInitializer.Actions
    ): AsyncJob<Unit> = asyncJob {
        delegate?.preInit(node, actions)?.invoke()?.merge()

        node.runtimeModel.partitions
            .filter { model -> model.type == MinecraftPartitionLoader.TYPE }
            .filter { model ->
                val enabled = (model.options["versions"]?.split(",") ?: listOf()).contains(
                    app.version
                )

                enabled
            }
            .forEach { model ->
                actions.addRequest(
                    PartitionArtifactRequest(node.descriptor, model.name),
                    extResolver.accessBridge.repositoryFor(node.descriptor),
                    extResolver.partitionResolver,
                )
            }
    }
}