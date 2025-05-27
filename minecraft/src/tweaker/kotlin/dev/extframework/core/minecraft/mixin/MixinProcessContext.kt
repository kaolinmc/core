package dev.extframework.core.minecraft.mixin

import dev.extframework.tooling.api.extension.ExtensionNode
import dev.extframework.tooling.api.extension.partition.ExtensionPartitionContainer

public data class MixinProcessContext(
    val node: ExtensionNode,
    val partitions: List<ExtensionPartitionContainer<*, *>>
)
