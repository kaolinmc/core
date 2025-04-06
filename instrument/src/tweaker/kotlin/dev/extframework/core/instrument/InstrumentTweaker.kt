package dev.extframework.core.instrument

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import dev.extframework.core.app.TargetLinker
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.instrument.internal.InstrumentedAppImpl
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.environment.MutableObjectSetAttribute
import dev.extframework.tooling.api.environment.extract
import dev.extframework.tooling.api.tweaker.EnvironmentTweaker

public class InstrumentTweaker : EnvironmentTweaker {
    override fun tweak(environment: ExtensionEnvironment): Job<Unit> = job {
        environment += MutableObjectSetAttribute(instrumentAgentsAttrKey)

        environment += InstrumentedAppImpl(
            environment[ApplicationTarget].extract(),
            environment[TargetLinker].extract(),
            environment[instrumentAgentsAttrKey].extract()
        )
    }
}