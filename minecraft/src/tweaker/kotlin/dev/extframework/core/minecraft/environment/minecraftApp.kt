package dev.extframework.core.minecraft.environment

import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.instrument.internal.InstrumentedAppImpl
import dev.extframework.core.minecraft.MinecraftApp
import dev.extframework.tooling.api.environment.ExtensionEnvironment

public val ExtensionEnvironment.minecraft: MinecraftApp
    get() {
        val instrumented = get(ApplicationTarget).get().getOrThrow() as InstrumentedAppImpl
        val minecraft = instrumented.delegate as MinecraftApp

        return minecraft
    }