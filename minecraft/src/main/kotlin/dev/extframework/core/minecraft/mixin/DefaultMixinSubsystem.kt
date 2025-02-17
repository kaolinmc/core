package dev.extframework.core.minecraft.mixin

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import dev.extframework.archives.ArchiveReference
import dev.extframework.core.minecraft.internal.remap.MixinMappingManager
import dev.extframework.core.minecraft.partition.MinecraftPartitionMetadata
import dev.extframework.core.minecraft.util.parseNode
import dev.extframework.mixin.MixinEngine
import dev.extframework.mixin.api.ClassReference.Companion.ref
import dev.extframework.mixin.api.Mixin
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.extension.partition.ExtensionPartitionContainer
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode

public class DefaultMixinSubsystem(
    public val engine: MixinEngine,
    public val mixinMappingManager: MixinMappingManager,
) : MixinSubsystem {
    override fun process(ctx: MixinProcessContext): Job<Boolean> = job {
        val (node) = ctx

        node.partitions
            .asSequence()
            .map(ExtensionPartitionContainer<*, *>::metadata)
            .filterIsInstance<MinecraftPartitionMetadata>()
            .forEach { metadata ->
                val archive = metadata.archive ?: return@forEach

                archive.reader
                    .entries()
                    .filter { it.name.endsWith(".class") }
                    .forEach { entry ->
                        val mixinNode = entry.open()
                            .parseNode()

                        val isMixin = (mixinNode.visibleAnnotations ?: listOf())
                            .any { it.desc == Type.getType(Mixin::class.java).descriptor }

                        if (isMixin) {
                            engine.registerMixin(mixinNode)
                            mixinMappingManager.tag(mixinNode.ref(), metadata.mappingNamespace)
                        }

                        isMixin
                    }
            }

        true
    }

    override fun transformClass(name: String, node: ClassNode?): ClassNode? {
        if (node == null) return null
        engine.transform(name, node)
        return node
    }
}