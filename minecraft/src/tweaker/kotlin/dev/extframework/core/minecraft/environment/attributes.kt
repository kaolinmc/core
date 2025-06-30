package dev.extframework.core.minecraft.environment

import dev.extframework.archive.mapper.MappingsProvider
import dev.extframework.core.minecraft.api.MappingNamespace
import dev.extframework.core.minecraft.remap.ExtensionRemapper
import dev.extframework.core.minecraft.remap.InjectionRemapper
import dev.extframework.mixin.MixinEngine
import dev.extframework.tooling.api.environment.MutableSetAttribute
import dev.extframework.tooling.api.environment.ValueAttribute

public val mappingProvidersAttrKey: MutableSetAttribute.Key<MappingsProvider> =
    MutableSetAttribute.Key("mapping-providers")
public val remappersAttrKey: MutableSetAttribute.Key<ExtensionRemapper> =
    MutableSetAttribute.Key("remappers")
public val mappingTargetAttrKey : ValueAttribute.Key<MappingNamespace> = ValueAttribute.Key("mapping-target")
public val engineAttrKey: ValueAttribute.Key<MixinEngine> = ValueAttribute.Key("mixin-engine")
public val redefinitionAttrKey: ValueAttribute.Key<String> = ValueAttribute.Key("redefinition-type")
public val injectionRemappersAttrKey: MutableSetAttribute.Key<InjectionRemapper<*>> =
    MutableSetAttribute.Key("injection-remappers")

//public val mixinAgentsAttrKey: MutableObjectSetAttribute.Key<MixinAgent> = MutableObjectSetAttribute.Key("mixin-agents")