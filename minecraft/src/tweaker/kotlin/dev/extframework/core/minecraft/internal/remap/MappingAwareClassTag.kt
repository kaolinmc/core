package dev.extframework.core.minecraft.internal.remap

import dev.extframework.core.minecraft.api.MappingNamespace
import dev.extframework.mixin.api.ClassReference
import dev.extframework.mixin.engine.tag.ClassTag

public data class MappingAwareClassTag(
    override val reference: ClassReference,
    val namespace: MappingNamespace,
) : ClassTag