package com.kaolinmc.core.minecraft.mixin

import com.kaolinmc.core.instrument.InstrumentedApplicationTarget
import com.kaolinmc.core.minecraft.internal.remap.MappingAwareClassTag
import com.kaolinmc.core.minecraft.internal.remap.MixinMappingManager
import com.kaolinmc.core.minecraft.partition.MinecraftPartitionMetadata
import com.kaolinmc.core.minecraft.util.parseNode
import com.kaolinmc.mixin.MixinEngine
import com.kaolinmc.mixin.api.ClassReference
import com.kaolinmc.mixin.api.ClassReference.Companion.ref
import com.kaolinmc.mixin.api.Mixin
import com.kaolinmc.mixin.engine.tag.ClassTag
import com.kaolinmc.tooling.api.extension.partition.ExtensionPartitionContainer
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import kotlin.sequences.flatMap
import kotlin.sequences.forEach

public class DefaultMixinSubsystem(
    public val engine: MixinEngine,
    public val mixinMappingManager: MixinMappingManager,
    public val app: InstrumentedApplicationTarget
) : MixinSubsystem {
    private val needPreprocessing = HashSet<ClassReference>()
    private val preprocessed = HashMap<ClassReference, ClassNode>()

    override fun unregister(tags: Set<ClassTag>) {
        val needReloading = tags.flatMap {
            engine.unregisterMixin(it)
        }
        for (reference in needReloading) {
            app.redefine(reference.name)
        }
    }

    override fun register(ctx: MixinProcessContext): Set<ClassTag> {
        val tags = resolveMixins(ctx)
            .mapTo(HashSet()) { (it, metadata) ->
                val tag = MappingAwareClassTag(it.ref(), metadata.mappingNamespace)
                val targets = engine.registerMixin(
                    tag,
                    it
                )

                needPreprocessing.addAll(targets)

                tag
            }

        return tags
    }

    private fun resolveMixins(
        ctx: MixinProcessContext,
    ): Sequence<Pair<ClassNode, MinecraftPartitionMetadata>> {
        return ctx.partitions
            .asSequence()
            .map(ExtensionPartitionContainer<*, *>::metadata)
            .filterIsInstance<MinecraftPartitionMetadata>()
            .flatMap { metadata ->
                val archive = metadata.archive ?: return@flatMap emptySequence()

                archive.reader
                    .entries()
                    .filter { it.name.endsWith(".class") }
                    .map { entry ->
                        entry.open().parseNode()
                    }.filter { mixinNode ->
                        (mixinNode.visibleAnnotations ?: listOf())
                            .any { it.desc == Type.getType(Mixin::class.java).descriptor }
                    }.map {
                        it to metadata
                    }
            }
    }

    override fun runPreprocessors() {
        for (reference in needPreprocessing.toSet()) {
            needPreprocessing.remove(reference)

            // The instrumented app API should already transform this.
            app.node.handle
                ?.classloader
                ?.getResourceAsStream("${reference.internalName}.class")
                ?.use { it ->
                    val node = ClassNode()
                    ClassReader(it).accept(node, ClassReader.EXPAND_FRAMES)

                    preprocessed[reference] = node
                    app.redefine(reference.name)
                }
        }
    }

    override fun transformClass(name: String, node: ClassNode?): ClassNode? {
        val reference = ClassReference(name)

        // TODO I took this out because in the case of fabric-ext loading mixins after extension
        //    initialization, this means anything transformed by a kaolin mixin isnt
        //    retransformed by fabric.
//        val preprocessed = this@DefaultMixinSubsystem.preprocessed[reference]
//        if (preprocessed != null) return preprocessed

        return if (node != null) {
            engine.transform(node)
        } else {
            engine.generate(reference)
        }
    }
}