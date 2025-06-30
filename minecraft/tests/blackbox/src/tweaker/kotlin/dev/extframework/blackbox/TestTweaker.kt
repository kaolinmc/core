package dev.extframework.blackbox

import dev.extframework.core.minecraft.MinecraftTweaker
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.tweaker.EnvironmentTweaker

class TestTweaker : EnvironmentTweaker {
    override fun tweak(environment: ExtensionEnvironment) {
        MinecraftTweaker()
    }
}