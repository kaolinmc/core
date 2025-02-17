package dev.extframework.core.minecraft.mixin

import com.durganmcbroom.jobs.Job
import dev.extframework.core.instrument.InstrumentAgent

public interface MixinSubsystem : InstrumentAgent {
    public fun process(
        ctx: MixinProcessContext
    ) : Job<Boolean>
}