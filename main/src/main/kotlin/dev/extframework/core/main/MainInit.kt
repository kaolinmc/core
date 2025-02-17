package dev.extframework.core.main

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import com.durganmcbroom.jobs.mapException
import com.durganmcbroom.jobs.result
import dev.extframework.tooling.api.exception.StructuredException
import dev.extframework.tooling.api.extension.ExtensionInitializer
import dev.extframework.tooling.api.extension.ExtensionNode
import dev.extframework.tooling.api.extension.descriptor
import dev.extframework.tooling.api.extension.partition.ExtensionPartitionContainer

public class MainInit(
    public val delegate: ExtensionInitializer?
) : ExtensionInitializer {
    // A map of our subsystems. Any or Object is included additionally as a reference to the default.

    override fun init(node: ExtensionNode): Job<Unit> = job {
        // Process mixins
        delegate?.init(node)?.invoke()?.merge()

        // Run init on main partitions
        result {
            val mainPartition = (node.partitions.find {
                it.metadata.name == "main"
            } as? ExtensionPartitionContainer<MainPartitionNode, MainPartitionMetadata>)

            mainPartition?.node?.entrypoint?.init()
        }.mapException {
            StructuredException(
                ExtensionInitialization,
                cause = it,
                message = "Exception initializing extension"
            ) {
                node.runtimeModel.descriptor.name asContext "Extension name"
            }
        }.getOrThrow()
    }
}