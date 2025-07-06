package com.kaolinmc.core.main

import com.kaolinmc.kiln.api.MutablePartitionRuntimeModel
import com.kaolinmc.kiln.api.NamedDomainPartitionContainer
import com.kaolinmc.kiln.api.util.newListProperty
import com.kaolinmc.kiln.api.util.newMapProperty
import com.kaolinmc.kiln.api.util.newSetProperty
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