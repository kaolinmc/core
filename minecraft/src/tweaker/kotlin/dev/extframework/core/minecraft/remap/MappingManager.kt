package dev.extframework.core.minecraft.remap

import dev.extframework.archive.mapper.MappingsProvider
import dev.extframework.archive.mapper.findShortest
import dev.extframework.archive.mapper.newMappingsGraph
import dev.extframework.common.util.LazyMap
import dev.extframework.core.minecraft.api.MappingNamespace
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.environment.ExtensionEnvironment.Attribute
import dev.extframework.tooling.api.environment.ExtensionEnvironment.Attribute.Key
import dev.extframework.tooling.api.environment.MutableObjectSetAttribute

public class MappingManager(
    private val mappingProviders: MutableObjectSetAttribute<MappingsProvider>,
    private val target: MappingNamespace,
    private val strict: Boolean = true,
    private val mcVersion: String
) : Attribute {
    override val key: Key<*> = MappingManager

    private val cache = LazyMap<Pair<MappingNamespace, MappingNamespace>, MappingContext> {
        MappingContext(
            newMappingsGraph(mappingProviders.toList(), strict).findShortest(
                it.first.identifier,
                it.second.identifier,
            ).forIdentifier(mcVersion),
            it.first.identifier,
            it.second.identifier,
        )

    }

    public operator fun get(source: MappingNamespace): MappingContext {
        return getTo(source, target)
    }

    public fun getTo(source: MappingNamespace, target: MappingNamespace): MappingContext {
        return cache[source to target]!!
    }

    public companion object : ExtensionEnvironment.Attribute.Key<MappingManager>
}