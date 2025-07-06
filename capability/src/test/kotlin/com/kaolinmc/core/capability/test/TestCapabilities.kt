package com.kaolinmc.core.capability.test

import com.kaolinmc.core.capability.Capabilities
import com.kaolinmc.core.capability.Capability
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

object TestCapabilities : Capabilities {
    private val capabilities: MutableMap<Capability.Reference<*>, Capability> = HashMap()

    private class DefinitionDelegate<T: Capability>(
        private val type: Class<T>,
    ) : Capabilities.DefinitionDelegate<T> {
        override fun getValue(
            thisRef: Any?,
            property: KProperty<*>
        ): Capability.Reference<T> {
            return define(property.name, type)
        }
    }

    override fun <T : Capability> definitionDelegate(type: KClass<T>): Capabilities.DefinitionDelegate<T> {
        return DefinitionDelegate(type.java)
    }

    override fun <T : Capability> register(
        ref: Capability.Reference<T>,
        impl: T
    ) {
        capabilities[ref] = impl
    }

    override fun <T : Capability> get(ref: Capability.Reference<T>): T {
        return (capabilities[ref] ?: throw Exception("Capability not found!")) as T
    }

    @JvmStatic
    @JvmOverloads
    fun <T : Capability> defineCapability(
        name: String = UUID.randomUUID().toString(),
        type: Class<out Capability>
    ) : Capability.Reference<T> {
        return define(name, type)
    }

    @JvmStatic
    fun <T : Capability> registerCapability(
        a: Capability.Reference<T>,
        b: T
    ): Unit = register(a, b)

    @JvmStatic
    fun <T : Capability> getCapability(ref: Capability.Reference<T>): T {
        return get(ref)
    }
}