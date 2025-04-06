package dev.extframework.core.minecraft.internal.remap

import dev.extframework.archive.mapper.transform.mapClassName
import dev.extframework.archive.mapper.transform.mapFieldName
import dev.extframework.archive.mapper.transform.mapMethodDesc
import dev.extframework.archive.mapper.transform.mapMethodName
import dev.extframework.core.minecraft.remap.InjectionRemapper
import dev.extframework.core.minecraft.remap.MappingContext
import dev.extframework.mixin.api.ClassReference.Companion.ref
import dev.extframework.mixin.api.InstructionSelector
import dev.extframework.mixin.engine.impl.code.BlockInjectionPoint
import dev.extframework.mixin.engine.impl.code.FieldAccessSelector
import dev.extframework.mixin.engine.impl.code.InstructionInjectionData
import dev.extframework.mixin.engine.impl.code.InvocationSelector
import dev.extframework.mixin.engine.impl.code.SingleInjectionPoint
import dev.extframework.mixin.engine.impl.code.findTargetMethod
import dev.extframework.mixin.engine.util.method
import org.objectweb.asm.commons.Method
import org.objectweb.asm.tree.ClassNode

internal class CodeInjectionRemapper : InjectionRemapper<InstructionInjectionData> {
    override val type: Class<InstructionInjectionData> = InstructionInjectionData::class.java
    override fun isTarget(
        data: InstructionInjectionData,
        node: ClassNode
    ): Boolean {
        return data.targets.contains(node.ref())
    }

    override fun remap(
        data: InstructionInjectionData,
        destination: ClassNode,
        context: MappingContext
    ): InstructionInjectionData {
        val unmappedMethodOptions = destination.methods
            .map { node -> node.method() }
            .associateBy { method ->
                context.mappings.mapMethodName(
                    destination.name,
                    method.name,
                    method.descriptor,
                    context.target,
                    context.source
                )?.let { name: String ->
                    Method(
                        name,
                        context.mappings.mapMethodDesc(
                            method.descriptor,
                            context.target, context.source
                        )
                    )
                } ?: method
            }

        val targetMethod = findTargetMethod(
            data.inferredTarget,
            unmappedMethodOptions.keys.toList(),
        ) { data.mixinClass to destination.ref() }

        val point = data.point
        return InstructionInjectionData(
            data.mixinClass,
            data.mixinMethod,
            unmappedMethodOptions[targetMethod]!!,
            data.injectionType,
            when (point) {
                is SingleInjectionPoint -> {
                    SingleInjectionPoint(
                        remapSelector(point.selector, context),
                        point.ordinal,
                        point.count
                    )
                }

                is BlockInjectionPoint -> {
                    BlockInjectionPoint(
                        remapSelector(point.start, context),
                        remapSelector(point.end, context),
                        point.ordinal,
                        point.count
                    )
                }

                else -> point
            },
            data.capturedLocals,
            data.targets
        )
    }

    private fun remapSelector(
        selector: InstructionSelector,
        context: MappingContext,
    ): InstructionSelector {
        return when (selector) {
            is FieldAccessSelector -> {
                val sourceOwner =
                    context.mappings.mapClassName(selector.owner.internalName, context.target, context.source)
                        ?: selector.owner.internalName

                FieldAccessSelector(
                    // Will get automatically remapped since it's a 'Type'
                    selector.owner,
                    context.mappings.mapFieldName(
                        sourceOwner,
                        selector.name,
                        context.source,
                        context.target
                    ) ?: selector.name,
                    selector.access
                )
            }

            is InvocationSelector -> {
                val sourceOwner =
                    context.mappings.mapClassName(selector.owner.internalName, context.target, context.source)
                        ?: selector.owner.internalName

                InvocationSelector(
                    // Will get automatically remapped since it's a 'Type'
                    selector.owner,
                    context.mappings.mapMethodName(
                        sourceOwner,
                        selector.method.name,
                        selector.method.descriptor,
                        context.source,
                        context.target
                    )?.let { name: String ->
                        Method(
                            name,
                            context.mappings.mapMethodDesc(
                                selector.method.descriptor,
                                context.source, context.target
                            )
                        )
                    } ?: selector.method,
                    selector.opcode
                )
            }

            else -> selector
        }
    }
}