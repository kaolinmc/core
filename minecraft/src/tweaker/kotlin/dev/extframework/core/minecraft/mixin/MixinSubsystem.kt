package dev.extframework.core.minecraft.mixin

import dev.extframework.core.instrument.InstrumentAgent

public interface MixinSubsystem : InstrumentAgent {
    public fun unregister(
        ctx: MixinProcessContext
    )

    public fun register(
        ctx: MixinProcessContext
    ): Boolean

    public fun runPreprocessors()
}