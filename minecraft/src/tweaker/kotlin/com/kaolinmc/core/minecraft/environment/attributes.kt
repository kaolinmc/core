package com.kaolinmc.core.minecraft.environment

import com.kaolinmc.archive.mapper.MappingsProvider
import com.kaolinmc.core.minecraft.api.MappingNamespace
import com.kaolinmc.core.minecraft.remap.ExtensionRemapper
import com.kaolinmc.core.minecraft.remap.InjectionRemapper
import com.kaolinmc.mixin.MixinEngine
import com.kaolinmc.tooling.api.environment.MutableSetAttribute
import com.kaolinmc.tooling.api.environment.ValueAttribute

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