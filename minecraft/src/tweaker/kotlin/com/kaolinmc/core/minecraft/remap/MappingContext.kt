package com.kaolinmc.core.minecraft.remap

import com.kaolinmc.archive.mapper.ArchiveMapping

public data class MappingContext(
    val mappings: ArchiveMapping,
    val source: String,
    val target: String,
)