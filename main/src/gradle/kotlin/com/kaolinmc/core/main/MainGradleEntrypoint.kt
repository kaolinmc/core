package com.kaolinmc.core.main

import com.kaolinmc.boot.monad.toList
import com.kaolinmc.kiln.api.*
import com.kaolinmc.kiln.api.source.SourcesManager
import com.kaolinmc.tooling.api.ExtensionLoader
import com.kaolinmc.tooling.api.extension.partition.artifact.PartitionArtifactRequest
import com.kaolinmc.tooling.api.extension.partition.artifact.partition

public class MainGradleEntrypoint : GradleEntrypoint {
    override suspend fun configure(
        extension: KaolinExtension,
        helper: GradleEntrypoint.Helper
    ) {
        helper.tweak(extension.rootEnvironment)

        val main = extension.partitions.findByName("main")

        if (main != null) {
            val loader = extension.rootEnvironment[ExtensionLoader]

            val partitionRequest = PartitionArtifactRequest(
                extension.model.descriptor.partition(
                    "main",
                )
            )
            val dependencies = loader.graph.cache(
                partitionRequest,
                helper.repository,
                loader.extensionResolver.partitionResolver
            )
                .parents
                .flatMap { it.toList() }

            helper.attachDependencies(
                main,
                dependencies
            )

            // ------ Sources ------
            val sources = extension.rootEnvironment[SourcesManager]
            sources.graph.cache(
                partitionRequest,
                helper.repository,
                sources.partitionResolver
            ).parents.flatMap { it.toList() }
        }

    }
}