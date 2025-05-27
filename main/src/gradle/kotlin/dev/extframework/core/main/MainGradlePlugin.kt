package dev.extframework.core.main

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import dev.extframework.boot.monad.toList
import dev.extframework.gradle.api.*
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.extension.partition.artifact.PartitionArtifactRequest
import dev.extframework.tooling.api.extension.partition.artifact.partition
import org.gradle.api.Project

public class MainGradlePlugin : GradleEntrypoint {
    override fun apply(project: Project) { }

    override fun tweak(root: BuildEnvironment): Job<Unit> = job {
        MainPartitionTweaker().tweak(root)().merge()

        root[environmentConfigurators].add(object : BuildEnvironmentConfigurator {
            override fun configure(
                environment: BuildEnvironment,
                helper: BuildEnvironmentConfigurator.Helper
            ): Job<Unit> = job {
                val main = environment.extension.partitions.findByName("main")

                if (main != null) {
                    val loader = environment.extension.loader

                    val dependencies = loader.graph.cache(
                        PartitionArtifactRequest(
                            environment.extension.model.descriptor.partition(
                                "main",
                                environment.extension.defaultEnvironment.name
                            )
                        ),
                        helper.repository,
                        loader.extensionResolver.partitionResolver
                    )().merge()
                        .parents
                        .flatMap { it.toList() }

                    helper.attachDependencies(
                        main,
                        dependencies
                    )
                }
            }
        })
    }
}