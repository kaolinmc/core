package dev.extframework.core.app.internal

import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.environment.ValueAttribute

public val internalExtraAppConfigAttrKey: ValueAttribute.Key<(ExtensionEnvironment) -> Unit> = ValueAttribute.Key(
    "internal-app-extra-config"
)