package dev.extframework.core.minecraft.environment

import dev.extframework.archive.mapper.MappingsProvider
import dev.extframework.core.minecraft.api.MappingNamespace
import dev.extframework.core.minecraft.remap.ExtensionRemapper
import dev.extframework.core.minecraft.remap.InjectionRemapper
import dev.extframework.mixin.MixinEngine
import dev.extframework.tooling.api.environment.MutableObjectSetAttribute
import dev.extframework.tooling.api.environment.ValueAttribute

public val mappingProvidersAttrKey: MutableObjectSetAttribute.Key<MappingsProvider> =
    MutableObjectSetAttribute.Key("mapping-providers")
public val remappersAttrKey: MutableObjectSetAttribute.Key<ExtensionRemapper> =
    MutableObjectSetAttribute.Key("remappers")
public val mappingTargetAttrKey : ValueAttribute.Key<MappingNamespace> = ValueAttribute.Key("mapping-target")
public val engineAttrKey: ValueAttribute.Key<MixinEngine> = ValueAttribute.Key("mixin-engine")
public val redefinitionAttrKey: ValueAttribute.Key<String> = ValueAttribute.Key("redefinition-type")
public val injectionRemappersAttrKey: MutableObjectSetAttribute.Key<InjectionRemapper<*>> =
    MutableObjectSetAttribute.Key("injection-remappers")

//public val mixinAgentsAttrKey: MutableObjectSetAttribute.Key<MixinAgent> = MutableObjectSetAttribute.Key("mixin-agents")