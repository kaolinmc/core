package com.kaolinmc.core.instrument

import com.kaolinmc.core.app.TargetLinker
import com.kaolinmc.core.app.api.ApplicationTarget
import com.kaolinmc.core.instrument.internal.InstrumentedAppImpl
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment
import com.kaolinmc.tooling.api.environment.MutableListAttribute
import com.kaolinmc.tooling.api.tweaker.EnvironmentTweaker

public class InstrumentTweaker : EnvironmentTweaker {
    override fun tweak(environment: ExtensionEnvironment) {
        environment += MutableListAttribute(instrumentAgentsAttrKey)

        environment += InstrumentedAppImpl(
            environment[ApplicationTarget],
            environment[TargetLinker],
            environment[instrumentAgentsAttrKey]
        )
    }
}