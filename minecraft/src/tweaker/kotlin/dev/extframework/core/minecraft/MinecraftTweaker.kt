package dev.extframework.core.minecraft

import dev.extframework.common.util.resolve
import dev.extframework.core.app.TargetLinker
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.instrument.InstrumentedApplicationTarget
import dev.extframework.core.instrument.instrumentAgentsAttrKey
import dev.extframework.core.main.MainPartitionMetadata
import dev.extframework.core.main.MainPartitionNode
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
import dev.extframework.core.minecraft.partition.MinecraftPartitionMetadata
import dev.extframework.core.minecraft.partition.MinecraftPartitionNode
import dev.extframework.core.minecraft.remap.MappingManager
import dev.extframework.minecraft.client.api.MinecraftExtensionInitializer
import dev.extframework.tooling.api.ExtensionLoader
import dev.extframework.tooling.api.environment.*
import dev.extframework.tooling.api.extension.ExtensionNode
import dev.extframework.tooling.api.extension.ExtensionUnloader
import dev.extframework.tooling.api.extension.partition.ExtensionPartitionContainer
import dev.extframework.tooling.api.extension.partition.artifact.partition
import dev.extframework.tooling.api.tweaker.EnvironmentTweaker

public class MinecraftTweaker : EnvironmentTweaker {
    override fun tweak(
        environment: ExtensionEnvironment
    ) {
        if (!environment.contains(mappingTargetAttrKey)) environment.set(
            ValueAttribute(
                mappingTargetAttrKey,
                MojangMappingProvider.OBF_TYPE,
            )
        )

        // Mapping providers
        environment += MutableSetAttribute(mappingProvidersAttrKey)
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
        )

        environment += mcApp

        environment[TargetLinker].target = mcApp

        val linker = environment[TargetLinker]

        val mcExtInitializer = McExtInitializer(
            environment[instrumentAgentsAttrKey],
            linker,
            environment.find(MinecraftExtensionInitializer),
            mcApp.delegate as MinecraftApp,
            environment[ExtensionLoader].extensionResolver,
            environment[ExtensionLoader].graph,
            environment.name,
        )
        environment += mcExtInitializer

        environment += MappingManager(
            environment,
            true,
            (mcApp.delegate as MinecraftAppApi).version
        )

        registerMixins(environment)

        // Minecraft partition
        val partitionContainer = environment[partitionLoadersAttrKey].container
        partitionContainer.register(MinecraftPartitionLoader(environment))

        // Remappers
        val remappers = MutableSetAttribute(remappersAttrKey)
        remappers.add(RootRemapper())
        environment += remappers

        val delegateCleaner = environment.find(ExtensionUnloader)
        environment += object : ExtensionUnloader {
            override fun cleanup(
                nodes: List<ExtensionNode>
            ) {
                delegateCleaner?.cleanup(nodes)

                for (node in nodes) {
                    val partitions = node.runtimeModel.partitions
                        .map { node.descriptor.partition(it.name) }
                        .mapNotNull { environment[ExtensionLoader].graph.nodes[it]?.value }
                        .filterIsInstance<ExtensionPartitionContainer<*, *>>()

                    environment[instrumentAgentsAttrKey]
                        .filterIsInstance<MixinSubsystem>()
                        .forEach {
                            it.unregister(
                                mcExtInitializer.extensionMixins[node.descriptor] ?: setOf()
                            )
                        }

                    partitions
                        .map { it.node }
                        .filterIsInstance<MinecraftPartitionNode>()
                        .forEach { t ->
                            t.entrypoint?.cleanup()
                        }

                    partitions
                        .map { it.node }
                        .filterIsInstance<MainPartitionNode>()
                        .forEach { t ->
                            t.entrypoint?.cleanup()
                        }

                    linker.extensionClasses.remove(node.descriptor)
                    linker.extensionResources.remove(node.descriptor)

                    partitions.forEach {
                        val metadata = it.metadata
                        when (metadata) {
                            is MinecraftPartitionMetadata -> metadata.archive?.close()
                            is MainPartitionMetadata -> metadata.archive?.close()
                        }
                    }
                }
            }
        }
    }
}