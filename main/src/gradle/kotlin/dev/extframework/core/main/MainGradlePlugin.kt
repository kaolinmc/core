package dev.extframework.core.main

import dev.extframework.boot.monad.toList
import dev.extframework.gradle.api.*
import dev.extframework.tooling.api.extension.partition.artifact.PartitionArtifactRequest
import dev.extframework.tooling.api.extension.partition.artifact.partition
import org.gradle.api.Project

public class MainGradlePlugin : GradleEntrypoint {
    override fun apply(project: Project) { }

    override fun tweak(root: BuildEnvironment) {
        MainPartitionTweaker().tweak(root)

        root[environmentConfigurators].add(object : BuildEnvironmentConfigurator {
            override suspend fun configure(
                environment: BuildEnvironment,
                helper: BuildEnvironmentConfigurator.Helper
            ) {
                val main = environment.extension.partitions.findByName("main")

                if (main != null) {
                    val loader = environment.extension.loader

                    val partitionRequest = PartitionArtifactRequest(
                        environment.extension.model.descriptor.partition(
                            "main",
                            environment.extension.defaultEnvironment.name
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
                    environment.extension.sourcesGraph.cache(
                        partitionRequest,
                        helper.repository,
                        environment.extension.partitionSourceResolver
                    ).parents.flatMap { it.toList() }
                }
            }
        })
    }
}