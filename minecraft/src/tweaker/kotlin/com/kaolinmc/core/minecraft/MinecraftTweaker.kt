package com.kaolinmc.core.minecraft

import com.kaolinmc.common.util.resolve
import com.kaolinmc.core.app.TargetLinker
import com.kaolinmc.core.app.api.ApplicationTarget
import com.kaolinmc.core.instrument.InstrumentedApplicationTarget
import com.kaolinmc.core.instrument.instrumentAgentsAttrKey
import com.kaolinmc.core.main.MainPartitionMetadata
import com.kaolinmc.core.main.MainPartitionNode
import com.kaolinmc.core.minecraft.api.MinecraftAppApi
import com.kaolinmc.core.minecraft.environment.mappingProvidersAttrKey
import com.kaolinmc.core.minecraft.environment.mappingTargetAttrKey
import com.kaolinmc.core.minecraft.environment.remappersAttrKey
import com.kaolinmc.core.minecraft.internal.MojangMappingProvider
import com.kaolinmc.core.minecraft.internal.RootRemapper
import com.kaolinmc.core.minecraft.mixin.MixinProcessContext
import com.kaolinmc.core.minecraft.mixin.MixinSubsystem
import com.kaolinmc.core.minecraft.mixin.registerMixins
import com.kaolinmc.core.minecraft.partition.MinecraftPartitionLoader
import com.kaolinmc.core.minecraft.partition.MinecraftPartitionMetadata
import com.kaolinmc.core.minecraft.partition.MinecraftPartitionNode
import com.kaolinmc.core.minecraft.remap.MappingManager
import com.kaolinmc.minecraft.client.api.MinecraftExtensionInitializer
import com.kaolinmc.tooling.api.ExtensionLoader
import com.kaolinmc.tooling.api.environment.*
import com.kaolinmc.tooling.api.extension.ExtensionNode
import com.kaolinmc.tooling.api.extension.ExtensionUnloader
import com.kaolinmc.tooling.api.extension.partition.ExtensionPartitionContainer
import com.kaolinmc.tooling.api.extension.partition.artifact.partition
import com.kaolinmc.tooling.api.tweaker.EnvironmentTweaker

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