package com.kaolinmc.core.instrument

import com.kaolinmc.tooling.api.environment.MutableListAttribute
import com.kaolinmc.tooling.api.environment.MutableSetAttribute

public val instrumentAgentsAttrKey: MutableListAttribute.Key<InstrumentAgent> = MutableListAttribute.Key("instrument-agents")
