package com.kaolinmc.core.minecraft.mixin

import com.kaolinmc.core.instrument.InstrumentAgent
import com.kaolinmc.mixin.engine.tag.ClassTag

public interface MixinSubsystem : InstrumentAgent {
    public fun unregister(
        tags: Set<ClassTag>
    )

    public fun register(
        ctx: MixinProcessContext
    ): Set<ClassTag>

    public fun runPreprocessors()
}