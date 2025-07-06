package com.kaolinmc.core.minecraft.remap

import com.kaolinmc.archive.mapper.transform.ClassInheritanceTree
import com.kaolinmc.archives.transform.TransformerConfig
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment.Attribute

public interface  ExtensionRemapper : Attribute {
    override val key: Attribute.Key<*>
        get() = ExtensionRemapper

    public companion object : Attribute.Key<ExtensionRemapper>

    public val priority: Int
        get() = 0

    public fun remap(
        context: MappingContext,
        inheritanceTree: ClassInheritanceTree,
    ): TransformerConfig
}