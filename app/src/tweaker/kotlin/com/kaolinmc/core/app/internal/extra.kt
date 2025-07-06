package com.kaolinmc.core.app.internal

import com.kaolinmc.tooling.api.environment.ExtensionEnvironment
import com.kaolinmc.tooling.api.environment.ValueAttribute

public val internalExtraAppConfigAttrKey: ValueAttribute.Key<(ExtensionEnvironment) -> Unit> = ValueAttribute.Key(
    "internal-app-extra-config"
)