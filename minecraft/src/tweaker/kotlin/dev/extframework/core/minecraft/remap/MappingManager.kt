package dev.extframework.core.minecraft.remap

import dev.extframework.archive.mapper.findShortest
import dev.extframework.archive.mapper.newMappingsGraph
import dev.extframework.common.util.LazyMap
import dev.extframework.core.minecraft.api.MappingNamespace
import dev.extframework.core.minecraft.environment.mappingProvidersAttrKey
import dev.extframework.core.minecraft.environment.mappingTargetAttrKey
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.environment.ExtensionEnvironment.Attribute
import dev.extframework.tooling.api.environment.ExtensionEnvironment.Attribute.Key

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