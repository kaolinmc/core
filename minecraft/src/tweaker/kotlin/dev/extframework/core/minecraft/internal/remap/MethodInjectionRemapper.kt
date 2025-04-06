package dev.extframework.core.minecraft.internal.remap

import dev.extframework.core.minecraft.remap.InjectionRemapper
import dev.extframework.core.minecraft.remap.MappingContext
import dev.extframework.mixin.api.ClassReference.Companion.ref
import dev.extframework.mixin.engine.impl.method.MethodInjectionData
import org.objectweb.asm.tree.ClassNode

public class MethodInjectionRemapper : InjectionRemapper<MethodInjectionData> {
    override val type: Class<MethodInjectionData> = MethodInjectionData::class.java

    override fun isTarget(
        data: MethodInjectionData,
        node: ClassNode
    ): Boolean {
        return data.targets.contains(node.ref())
    }

    override fun remap(
        data: MethodInjectionData,
        destination: ClassNode,
        context: MappingContext
    ): MethodInjectionData {
        return data
    }
}