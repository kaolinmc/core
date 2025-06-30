package dev.extframework.core.minecraft.remap

import dev.extframework.archive.mapper.transform.ClassInheritanceTree
import dev.extframework.archives.transform.TransformerConfig
import dev.extframework.tooling.api.environment.ExtensionEnvironment.Attribute

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