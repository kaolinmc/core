package com.kaolinmc.core.main

import com.kaolinmc.kiln.api.EvaluatingDependency
import com.kaolinmc.kiln.api.MutablePartitionRuntimeModel
import com.kaolinmc.kiln.api.PartitionDependencyHandler
import com.kaolinmc.kiln.api.PartitionHandler
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet

public class MainPartitionHandler(
    project: Project,
    partition: MutablePartitionRuntimeModel,
    sourceSet: SourceSet, configure: (() -> Unit) -> Unit
) : PartitionHandler<PartitionDependencyHandler>(project, partition, sourceSet, configure) {
    override val dependencies: PartitionDependencyHandler = PartitionDependencyHandler(
        project.dependencies, sourceSet
    ) {
        partition.dependencies.add(it)
    }

    public var extensionClass: String
        get() {
            return model.options.getting("extension-class").get()
        }
        set(value) {
            model.options.put("extension-class", value)
        }
}