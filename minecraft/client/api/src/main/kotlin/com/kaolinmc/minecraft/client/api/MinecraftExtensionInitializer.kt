package com.kaolinmc.minecraft.client.api

import com.kaolinmc.tooling.api.environment.ExtensionEnvironment
import com.kaolinmc.tooling.api.environment.MutableSetAttribute
import com.kaolinmc.tooling.api.extension.ExtensionNode

public val minecraftInitializersAttrKey: MutableSetAttribute.Key<MinecraftExtensionInitializer> =
    MutableSetAttribute.Key("minecraft-initializers")

public interface MinecraftExtensionInitializer : ExtensionEnvironment.Attribute {
    override val key: ExtensionEnvironment.Attribute.Key<*>
        get() = MinecraftExtensionInitializer

    public companion object : ExtensionEnvironment.Attribute.Key<MinecraftExtensionInitializer>

    public suspend fun initialize(
        nodes: List<ExtensionNode>
    )
}