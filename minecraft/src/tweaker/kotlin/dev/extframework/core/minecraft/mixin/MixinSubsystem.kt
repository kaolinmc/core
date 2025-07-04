package dev.extframework.core.minecraft.mixin

import dev.extframework.core.instrument.InstrumentAgent
import dev.extframework.mixin.engine.tag.ClassTag

public interface MixinSubsystem : InstrumentAgent {
    public fun unregister(
        tags: Set<ClassTag>
    )

    public fun register(
        ctx: MixinProcessContext
    ): Set<ClassTag>

    public fun runPreprocessors()
}