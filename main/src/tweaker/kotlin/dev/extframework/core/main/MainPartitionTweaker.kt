package dev.extframework.core.main

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.environment.partitionLoadersAttrKey
import dev.extframework.tooling.api.extension.ExtensionNode
import dev.extframework.tooling.api.extension.ExtensionResolver
import dev.extframework.tooling.api.tweaker.EnvironmentTweaker

public class MainPartitionTweaker : EnvironmentTweaker {
    override fun tweak(
        environment: ExtensionEnvironment
    ): Job<Unit> = job {
        val partitionContainer = environment[partitionLoadersAttrKey].container
        partitionContainer.register("main", MainPartitionLoader())

        // Extension init
//        environment += MainInit(
//            environment[ExtensionInitializer].getOrNull(),
//            environment[ExtensionResolver].extract(),
//            environment.archiveGraph,
//        )

//    TODO    val delegateCleaner = environment[ExtensionCleaner].getOrNull()
//        environment += object : ExtensionCleaner {
//            override fun cleanup(nodes: List<ExtensionNode>): Job<Unit> = job() {
//                delegateCleaner?.cleanup(nodes)?.invoke()?.merge()
//
//                for (node in nodes) {
//                    node.partitions
//                        .map { it.node }
//                        .filterIsInstance<MainPartitionNode>()
//                        .forEach { t ->
//                            t.entrypoint?.cleanup()
//                        }
//                }
//            }
//        }
    }
}