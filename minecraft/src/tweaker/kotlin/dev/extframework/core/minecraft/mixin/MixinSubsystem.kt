package dev.extframework.core.minecraft.mixin

import com.durganmcbroom.jobs.Job
import dev.extframework.core.instrument.InstrumentAgent
import dev.extframework.tooling.api.environment.ExtensionEnvironment.Attribute
import dev.extframework.tooling.api.environment.ExtensionEnvironment.Attribute.Key

public interface MixinSubsystem : InstrumentAgent {
    public fun unregister(
        ctx: MixinProcessContext
    ): Job<Unit>

    public fun register(
        ctx: MixinProcessContext
    ): Job<Boolean>

    public fun runPreprocessors(): Job<Unit>
}