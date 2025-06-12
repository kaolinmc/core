package dev.extframework.core.minecraft

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import dev.extframework.common.util.resolve
import dev.extframework.core.app.TargetLinker
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.instrument.InstrumentedApplicationTarget
import dev.extframework.core.instrument.instrumentAgentsAttrKey
import dev.extframework.core.minecraft.api.MinecraftAppApi
import dev.extframework.core.minecraft.environment.mappingProvidersAttrKey
import dev.extframework.core.minecraft.environment.mappingTargetAttrKey
import dev.extframework.core.minecraft.environment.remappersAttrKey
import dev.extframework.core.minecraft.internal.MojangMappingProvider
import dev.extframework.core.minecraft.internal.RootRemapper
import dev.extframework.core.minecraft.mixin.registerMixins
import dev.extframework.core.minecraft.partition.MinecraftPartitionLoader
import dev.extframework.core.minecraft.remap.MappingManager
import dev.extframework.minecraft.client.api.MinecraftExtensionInitializer
import dev.extframework.tooling.api.ExtensionLoader
import dev.extframework.tooling.api.environment.*
import dev.extframework.tooling.api.tweaker.EnvironmentTweaker

public class MinecraftTweaker : EnvironmentTweaker {
    override fun tweak(
        environment: ExtensionEnvironment
    ): Job<Unit> = job {
        if (!environment.contains(mappingTargetAttrKey)) environment.set(
            ValueAttribute(
                MojangMappingProvider.OBF_TYPE,
                mappingTargetAttrKey
            )
        )

        // Mapping providers
        environment += MutableObjectSetAttribute(mappingProvidersAttrKey)
        environment[mappingProvidersAttrKey].add(
            MojangMappingProvider(
                environment[wrkDirAttrKey].value resolve "mappings"
            )
        )

        // Minecraft app, lazy delegation here so that mappings can process
        val mcApp = MinecraftApp(
            environment[ApplicationTarget] as? InstrumentedApplicationTarget
                ?: throw Exception("Illegal environment, the application must be instrumented. (Something is wrong in the extension loading process)"),
            environment
        )().merge()

        environment += mcApp

        environment[TargetLinker].target = mcApp

        val linker = environment[TargetLinker]

        environment += McExtInitializer(
            environment[instrumentAgentsAttrKey],
            linker,
            environment.find(MinecraftExtensionInitializer),
            mcApp.delegate as MinecraftApp,
            environment[ExtensionLoader].extensionResolver,
            environment[ExtensionLoader].graph,
            environment.name,
        )

        environment += MappingManager(
            environment,
            true,
            (mcApp.delegate as MinecraftAppApi).version
        )

        registerMixins(environment)

        // Minecraft partition
        val partitionContainer = environment[partitionLoadersAttrKey].container
        partitionContainer.register("minecraft", MinecraftPartitionLoader(environment))

        // Remappers
        val remappers = MutableObjectSetAttribute(remappersAttrKey)
        remappers.add(RootRemapper())
        environment += remappers

//        val delegateCleaner = environment[ExtensionCleaner].getOrNull()
//        environment += object : ExtensionCleaner {
//            override fun cleanup(
//                nodes: List<ExtensionNode>
//            ): Job<Unit> = job {
//                delegateCleaner?.cleanup(nodes)?.invoke()?.merge()
//
//                for (node in nodes) {
//                    environment[instrumentAgentsAttrKey].extract()
//                        .filterIsInstance<MixinSubsystem>()
//                        .forEach {
//                            it.unregister(
//                                MixinProcessContext(node)
//                            )().merge()
//                        }
//
//                    node.partitions
//                        .map { it.node }
//                        .filterIsInstance<MinecraftPartitionNode>()
//                        .forEach { t ->
//                            t.entrypoint?.cleanup()
//                        }
//                }
//            }
//        }
    }
}