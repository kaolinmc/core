package com.kaolinmc.core.capability

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

public interface Capabilities {
    public interface DefinitionDelegate<T : Capability> {
        public operator fun getValue(thisRef: Any?, property: KProperty<*>): Capability.Reference<T>
    }

    public fun <T : Capability> definitionDelegate(type: KClass<T>): DefinitionDelegate<T>

    public fun <T : Capability> define(
        name: String,
        type: Class<out Capability>
    ) : Capability.Reference<T> {
        return Capability.Reference(
            name, type as Class<T>, this
        )
    }

    public fun <T : Capability> register(
        ref: Capability.Reference<T>,
        impl: T
    )

    public fun <T : Capability> unregister(
        ref: Capability.Reference<T>,
    )

    public operator fun <T : Capability> get(
        ref: Capability.Reference<T>,
    ): T
}