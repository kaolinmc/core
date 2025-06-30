package dev.extframework.core.main

import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.environment.partitionLoadersAttrKey
import dev.extframework.tooling.api.tweaker.EnvironmentTweaker

public class MainPartitionTweaker : EnvironmentTweaker {
    override fun tweak(
        environment: ExtensionEnvironment
    ) {
        val partitionContainer = environment[partitionLoadersAttrKey].container
        partitionContainer.register(MainPartitionLoader())

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