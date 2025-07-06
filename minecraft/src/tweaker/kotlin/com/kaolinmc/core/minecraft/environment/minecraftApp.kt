package com.kaolinmc.core.minecraft.environment

import com.kaolinmc.core.app.api.ApplicationTarget
import com.kaolinmc.core.instrument.internal.InstrumentedAppImpl
import com.kaolinmc.core.minecraft.MinecraftApp
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment

public val ExtensionEnvironment.minecraft: MinecraftApp
    get() {
        val instrumented = get(ApplicationTarget) as InstrumentedAppImpl
        val minecraft = instrumented.delegate as MinecraftApp

        return minecraft
    }