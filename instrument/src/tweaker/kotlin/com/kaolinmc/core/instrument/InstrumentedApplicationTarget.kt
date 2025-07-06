package com.kaolinmc.core.instrument

import com.kaolinmc.core.app.api.ApplicationTarget

public interface InstrumentedApplicationTarget : ApplicationTarget {
    public val delegate: ApplicationTarget
    public val agents: List<InstrumentAgent>

    public fun registerAgent(agent: InstrumentAgent)

    public fun redefine(name: String)
}