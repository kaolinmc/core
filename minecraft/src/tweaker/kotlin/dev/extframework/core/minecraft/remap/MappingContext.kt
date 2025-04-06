package dev.extframework.core.minecraft.remap

import dev.extframework.archive.mapper.ArchiveMapping

public data class MappingContext(
    val mappings: ArchiveMapping,
    val source: String,
    val target: String,
)