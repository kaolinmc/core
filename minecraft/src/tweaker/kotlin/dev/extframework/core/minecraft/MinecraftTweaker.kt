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
import dev.extframework.core.minecraft.mixin.MixinProcessContext
import dev.extframework.core.minecraft.mixin.MixinSubsystem
import dev.extframework.core.minecraft.mixin.registerMixins
import dev.extframework.core.minecraft.partition.MinecraftPartitionLoader
import dev.extframework.core.minecraft.partition.MinecraftPartitionNode
import dev.extframework.core.minecraft.remap.MappingManager
import dev.extframework.tooling.api.environment.*
import dev.extframework.tooling.api.extension.ExtensionCleaner
import dev.extframework.tooling.api.extension.ExtensionInitializer
import dev.extframework.tooling.api.extension.ExtensionNode
import dev.extframework.tooling.api.extension.ExtensionResolver
import dev.extframework.tooling.api.tweaker.EnvironmentTweaker

public class MinecraftTweaker : EnvironmentTweaker {
    override fun tweak(
        environment: ExtensionEnvironment
    ): Job<Unit> = job {
        environment.setUnless(
            ValueAttribute(
                MojangMappingProvider.OBF_TYPE,
                mappingTargetAttrKey
            )
        )

        // Mapping providers
        environment += MutableObjectSetAttribute(mappingProvidersAttrKey)
        environment[mappingProvidersAttrKey].extract().add(
            MojangMappingProvider(
                environment[wrkDirAttrKey].extract().value resolve "mappings"
            )
        )

        // Minecraft app, lazy delegation here so that mappings can process
        val mcApp = MinecraftApp(
            environment[ApplicationTarget].extract() as? InstrumentedApplicationTarget
                ?: throw Exception("Illegal environment, the application must be instrumented. (Something is wrong in the extension loading process)"),
            environment
        )().merge()

        environment += mcApp

        val linker = environment[TargetLinker].extract()
        environment += MinecraftExtensionInitializer(
            environment[instrumentAgentsAttrKey].extract(),
            linker,
            environment[ExtensionInitializer].getOrNull(),

            mcApp.delegate as MinecraftApp,
            environment[ExtensionResolver].extract(),
            environment.archiveGraph,
        )

        environment += MappingManager(
            environment[mappingProvidersAttrKey].extract(),
            environment[mappingTargetAttrKey].map { attribute -> attribute.value },
            true,
            (mcApp.delegate as MinecraftAppApi).version
        )

        registerMixins(environment)

        // Minecraft partition
        val partitionContainer = environment[partitionLoadersAttrKey].extract().container
        partitionContainer.register("minecraft", MinecraftPartitionLoader(environment))

        // Remappers
        val remappers = MutableObjectSetAttribute(remappersAttrKey)
        remappers.add(RootRemapper())
        environment += remappers

        val delegateCleaner = environment[ExtensionCleaner].getOrNull()
        environment += object : ExtensionCleaner {
            override fun cleanup(
                nodes: List<ExtensionNode>
            ): Job<Unit> = job {
                delegateCleaner?.cleanup(nodes)?.invoke()?.merge()

                for (node in nodes) {
                    environment[instrumentAgentsAttrKey].extract()
                        .filterIsInstance<MixinSubsystem>()
                        .forEach {
                            it.unregister(
                                MixinProcessContext(node)
                            )().merge()
                        }

                    node.partitions
                        .map {it.node}
                        .filterIsInstance<MinecraftPartitionNode>()
                        .forEach { t ->
                            t.entrypoint?.cleanup()
                        }
                }
            }
        }
    }
}