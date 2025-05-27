package dev.extframework.core.minecraft.remap

import dev.extframework.archive.mapper.ArchiveMapping
import dev.extframework.archive.mapper.transform.ClassInheritanceTree
import dev.extframework.archives.transform.TransformerConfig
import dev.extframework.tooling.api.environment.ExtensionEnvironment.Attribute
import dev.extframework.tooling.api.environment.ExtensionEnvironment

public interface  ExtensionRemapper : ExtensionEnvironment.Attribute {
    override val key: ExtensionEnvironment.Attribute.Key<*>
        get() = ExtensionRemapper

    public companion object : ExtensionEnvironment.Attribute.Key<ExtensionRemapper>

    public val priority: Int
        get() = 0

    public fun remap(
        context: MappingContext,
        inheritanceTree: ClassInheritanceTree,
    ): TransformerConfig
}