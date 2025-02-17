package dev.extframework.core.minecraft.internal

import dev.extframework.archive.mapper.transform.ClassInheritanceTree
import dev.extframework.archive.mapper.transform.mappingTransformConfigFor
import dev.extframework.archives.transform.TransformerConfig
import dev.extframework.core.minecraft.remap.ExtensionRemapper
import dev.extframework.core.minecraft.remap.MappingContext

public class RootRemapper : ExtensionRemapper {
    override fun remap(
        context: MappingContext,
        inheritanceTree: ClassInheritanceTree
    ): TransformerConfig = mappingTransformConfigFor(
        context.mappings,
        context.source, context.target,
        inheritanceTree,
    )
}