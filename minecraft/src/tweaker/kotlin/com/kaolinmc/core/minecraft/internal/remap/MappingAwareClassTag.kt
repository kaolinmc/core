package com.kaolinmc.core.minecraft.internal.remap

import com.kaolinmc.core.minecraft.api.MappingNamespace
import com.kaolinmc.mixin.api.ClassReference
import com.kaolinmc.mixin.engine.tag.ClassTag

public data class MappingAwareClassTag(
    override val reference: ClassReference,
    val namespace: MappingNamespace,
) : ClassTag