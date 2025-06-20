package dev.extframework.core.minecraft.mixin

import dev.extframework.core.instrument.InstrumentedApplicationTarget
import dev.extframework.core.minecraft.internal.remap.MappingAwareClassTag
import dev.extframework.core.minecraft.internal.remap.MixinMappingManager
import dev.extframework.core.minecraft.partition.MinecraftPartitionMetadata
import dev.extframework.core.minecraft.util.parseNode
import dev.extframework.mixin.MixinEngine
import dev.extframework.mixin.api.ClassReference
import dev.extframework.mixin.api.ClassReference.Companion.ref
import dev.extframework.mixin.api.Mixin
import dev.extframework.tooling.api.extension.partition.ExtensionPartitionContainer
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

    override fun unregister(ctx: MixinProcessContext) {
        val needReloading = resolveMixins(ctx)
            .flatMap { (mixinNode, metadata) ->
                engine.unregisterMixin(
                    MappingAwareClassTag(
                        mixinNode.ref(),
                        metadata.mappingNamespace
                    )
                )
            }

        for (reference in needReloading) {
            app.redefine(reference.name)
        }
    }

    override fun register(ctx: MixinProcessContext): Boolean {
        resolveMixins(ctx)
            .forEach { (it, metadata) ->
                val targets = engine.registerMixin(
                    MappingAwareClassTag(it.ref(), metadata.mappingNamespace),
                    it
                )
                needPreprocessing.addAll(targets)
            }

        return true
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
//                    val node = if (it != null) {
//                        val node = ClassNode()
//                        ClassReader(it).accept(node, ClassReader.EXPAND_FRAMES)
//
//                        engine.transform(node)
//                    } else {
//                        engine.generate(reference)
//                    } ?: return@use

                    val node = ClassNode()
                    ClassReader(it).accept(node, ClassReader.EXPAND_FRAMES)

                    preprocessed[reference] = node
                }
        }
    }

    override fun transformClass(name: String, node: ClassNode?): ClassNode? {
        val reference = ClassReference(name)

        // TODO I took this out because in the case of fabric-ext loading mixins after extension
        //    initialization, this means anything transformed by a extframework mixin isnt
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