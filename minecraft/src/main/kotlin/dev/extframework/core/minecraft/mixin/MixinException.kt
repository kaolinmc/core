package dev.extframework.core.minecraft.mixin

import dev.extframework.tooling.api.exception.ExceptionConfiguration
import dev.extframework.tooling.api.exception.StructuredException

public fun MixinException(
    cause: Throwable? = null,
    message: String? = null,
    configure: ExceptionConfiguration.() -> Unit,
): Throwable = StructuredException(MixinErr, cause, message, configure)