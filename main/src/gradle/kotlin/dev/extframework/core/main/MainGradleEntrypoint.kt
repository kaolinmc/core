package dev.extframework.core.main

import dev.extframework.boot.monad.toList
import dev.extframework.gradle.api.*
import dev.extframework.gradle.api.source.SourcesManager
import dev.extframework.tooling.api.ExtensionLoader
import dev.extframework.tooling.api.extension.partition.artifact.PartitionArtifactRequest
import dev.extframework.tooling.api.extension.partition.artifact.partition

public class MainGradleEntrypoint : GradleEntrypoint {
    override suspend fun configure(
        extension: ExtframeworkExtension,
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