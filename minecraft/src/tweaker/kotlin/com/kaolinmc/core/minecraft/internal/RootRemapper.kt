package com.kaolinmc.core.minecraft.internal

import com.kaolinmc.archive.mapper.transform.ClassInheritanceTree
import com.kaolinmc.archive.mapper.transform.mappingTransformConfigFor
import com.kaolinmc.archives.transform.TransformerConfig
import com.kaolinmc.core.minecraft.remap.ExtensionRemapper
import com.kaolinmc.core.minecraft.remap.MappingContext

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