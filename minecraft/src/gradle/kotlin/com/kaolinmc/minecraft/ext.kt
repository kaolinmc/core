package com.kaolinmc.minecraft

import com.kaolinmc.kiln.api.MutablePartitionRuntimeModel
import com.kaolinmc.kiln.api.NamedDomainPartitionContainer
import com.kaolinmc.kiln.api.util.newListProperty
import com.kaolinmc.kiln.api.util.newMapProperty
import com.kaolinmc.kiln.api.util.newSetProperty
import org.gradle.api.Action

public fun NamedDomainPartitionContainer.minecraft(name: String, action: Action<MinecraftPartitionHandler>) {
    val partition = MutablePartitionRuntimeModel(
        "minecraft",
        name,
        extension.project.newListProperty(),
        extension.project.newSetProperty(),
        extension.project.newMapProperty()
    )

    doAdd(action) {
        val sourceSet = extension.sourceSets.create(name)

        MinecraftPartitionHandler(
            extension.project,
            partition,
            sourceSet,
            it
        )
    }
}