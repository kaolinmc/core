package com.kaolinmc.core.minecraft.internal.remap

import com.kaolinmc.core.minecraft.remap.InjectionRemapper
import com.kaolinmc.core.minecraft.remap.MappingContext
import com.kaolinmc.mixin.api.ClassReference.Companion.ref
import com.kaolinmc.mixin.engine.impl.method.MethodInjectionData
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