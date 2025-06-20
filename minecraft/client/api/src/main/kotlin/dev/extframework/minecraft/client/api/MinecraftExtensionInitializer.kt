package dev.extframework.minecraft.client.api

import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.environment.MutableObjectSetAttribute
import dev.extframework.tooling.api.extension.ExtensionNode

public val minecraftInitializersAttrKey: MutableObjectSetAttribute.Key<MinecraftExtensionInitializer> =
    MutableObjectSetAttribute.Key("minecraft-initializers")

public interface MinecraftExtensionInitializer : ExtensionEnvironment.Attribute {
    override val key: ExtensionEnvironment.Attribute.Key<*>
        get() = MinecraftExtensionInitializer

    public companion object : ExtensionEnvironment.Attribute.Key<MinecraftExtensionInitializer>

    public suspend fun initialize(
        nodes: List<ExtensionNode>
    )
}