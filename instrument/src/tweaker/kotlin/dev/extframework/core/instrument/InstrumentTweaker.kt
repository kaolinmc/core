package dev.extframework.core.instrument

import dev.extframework.core.app.TargetLinker
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.instrument.internal.InstrumentedAppImpl
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.environment.MutableListAttribute
import dev.extframework.tooling.api.tweaker.EnvironmentTweaker

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