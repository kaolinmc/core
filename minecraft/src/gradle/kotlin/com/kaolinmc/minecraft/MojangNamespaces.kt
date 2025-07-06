package com.kaolinmc.minecraft

import com.kaolinmc.core.minecraft.api.MappingNamespace

public object MojangNamespaces {
    @JvmStatic
    public val obfuscated: MappingNamespace = MappingNamespace("mojang", "obfuscated")

    @JvmStatic
    public val deobfuscated: MappingNamespace = MappingNamespace("mojang", "deobfuscated")
}