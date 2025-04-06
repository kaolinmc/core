package dev.extframework.core.main

import dev.extframework.gradle.api.MutablePartitionRuntimeModel
import dev.extframework.gradle.api.NamedDomainPartitionContainer
import dev.extframework.gradle.api.util.newListProperty
import dev.extframework.gradle.api.util.newMapProperty
import dev.extframework.gradle.api.util.newSetProperty
import org.gradle.api.Action

public fun NamedDomainPartitionContainer.main(action: Action<MainPartitionHandler>) {
    val partition = MutablePartitionRuntimeModel(
        "main",
        "main",
        extension.project.newListProperty(),
        extension.project.newSetProperty(),
        extension.project.newMapProperty()
    )

    doAdd(action) {
        val sourceSet = extension.sourceSets.getByName("main")

        MainPartitionHandler(
            extension.project,
            partition,
            sourceSet,
            it
        )
    }
}