package dev.extframework.minecraft

import dev.extframework.gradle.api.MutablePartitionRuntimeModel
import dev.extframework.gradle.api.NamedDomainPartitionContainer
import dev.extframework.gradle.api.util.newListProperty
import dev.extframework.gradle.api.util.newMapProperty
import dev.extframework.gradle.api.util.newSetProperty
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
            extension,
            it
        )
    }
}