package com.kaolinmc.core.minecraft.mixin

import com.kaolinmc.tooling.api.extension.ExtensionNode
import com.kaolinmc.tooling.api.extension.partition.ExtensionPartitionContainer

public data class MixinProcessContext(
    val node: ExtensionNode,
    val partitions: List<ExtensionPartitionContainer<*, *>>
)
