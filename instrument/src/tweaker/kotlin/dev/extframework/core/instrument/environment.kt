package dev.extframework.core.instrument

import dev.extframework.tooling.api.environment.MutableListAttribute
import dev.extframework.tooling.api.environment.MutableSetAttribute

public val instrumentAgentsAttrKey: MutableListAttribute.Key<InstrumentAgent> = MutableListAttribute.Key("instrument-agents")
