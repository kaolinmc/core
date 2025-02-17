package dev.extframework.core.minecraft

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import com.durganmcbroom.jobs.mapException
import com.durganmcbroom.jobs.result
import dev.extframework.boot.archive.ClassLoadedArchiveNode
import dev.extframework.boot.loader.ArchiveClassProvider
import dev.extframework.boot.loader.ArchiveResourceProvider
import dev.extframework.boot.loader.DelegatingClassProvider
import dev.extframework.boot.loader.DelegatingResourceProvider
import dev.extframework.core.app.TargetLinker
import dev.extframework.core.instrument.InstrumentAgent
import dev.extframework.core.main.ExtensionInitialization
import dev.extframework.core.minecraft.mixin.MixinProcessContext
import dev.extframework.core.minecraft.mixin.MixinSubsystem
import dev.extframework.core.minecraft.partition.MinecraftPartitionNode
import dev.extframework.tooling.api.environment.MutableObjectSetAttribute
import dev.extframework.tooling.api.exception.StructuredException
import dev.extframework.tooling.api.extension.ExtensionInitializer
import dev.extframework.tooling.api.extension.ExtensionNode
import dev.extframework.tooling.api.extension.descriptor

public class MinecraftExtensionInitializer(
    private val mixinSubsystems: MutableObjectSetAttribute<InstrumentAgent>,
    private val linker: TargetLinker,
    public val delegate: ExtensionInitializer?,
    private val app: MinecraftApp,
) : ExtensionInitializer {
    override fun init(node: ExtensionNode): Job<Unit> = job {
        // TODO this creates inefficiencies as subsystems will all independently have to iterate over all
        //  classes to see if they use their subsystem. Instead the mixin process context should be expanded
        //  to include a 'node' parameter in which they only process 1 item at a time.
        mixinSubsystems
            .filterIsInstance<MixinSubsystem>()
            .forEach {
                it.process(MixinProcessContext(node))().merge()
            }

        app.setup()().merge()

        // TODO this creates duplicates if two extensions rely on the same library.
        linker.addExtensionClasses(
            DelegatingClassProvider(
                node.partitions
                    .flatMap { it.access.targets.map { it.relationship.node } + it }
                    .filterIsInstance<ClassLoadedArchiveNode<*>>()
                    .map { it.handle }
                    .map { ArchiveClassProvider(it) })
        )
        linker.addExtensionResources(DelegatingResourceProvider(node.partitions.map { it.handle }
            .map { ArchiveResourceProvider(it) }))

        // Run init on target partitions
        node.partitions
            .forEach { container ->
                val partNode = container.node as? MinecraftPartitionNode ?: return@forEach

                result {
                    partNode.entrypoint?.init()
                }.mapException {
                    StructuredException(
                        ExtensionInitialization,
                        cause = it,
                        message = "Exception initializing minecraft partition"
                    ) {
                        node.runtimeModel.descriptor.name asContext "Extension name"
                        container.metadata.name asContext "Partition name"
                    }
                }.getOrThrow()
            }

        delegate?.init(node)?.invoke()?.merge()
    }
}