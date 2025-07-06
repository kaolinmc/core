package com.kaolinmc.core.minecraft.remap

import com.kaolinmc.archive.mapper.findShortest
import com.kaolinmc.archive.mapper.newMappingsGraph
import com.kaolinmc.common.util.LazyMap
import com.kaolinmc.core.minecraft.api.MappingNamespace
import com.kaolinmc.core.minecraft.environment.mappingProvidersAttrKey
import com.kaolinmc.core.minecraft.environment.mappingTargetAttrKey
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment.Attribute
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment.Attribute.Key

public class MappingManager(
    private val environment: ExtensionEnvironment,
    private val strict: Boolean = true,
    private val mcVersion: String
) : Attribute {
    override val key: Key<*> = MappingManager

    private val cache = LazyMap<Pair<MappingNamespace, MappingNamespace>, MappingContext> {
        MappingContext(
            newMappingsGraph(environment[mappingProvidersAttrKey].toList(), strict).findShortest(
                it.first.identifier,
                it.second.identifier,
            ).forIdentifier(mcVersion),
            it.first.identifier,
            it.second.identifier,
        )

    }

    public operator fun get(source: MappingNamespace): MappingContext {
        return getTo(source, environment[mappingTargetAttrKey].value)
    }

    public fun getTo(source: MappingNamespace, target: MappingNamespace): MappingContext {
        return cache[source to target]!!
    }

    public companion object : ExtensionEnvironment.Attribute.Key<MappingManager>
}