package com.kaolinmc.core.app

import com.kaolinmc.core.app.api.ApplicationTarget
import com.kaolinmc.core.app.internal.internalExtraAppConfigAttrKey
import com.kaolinmc.tooling.api.ExtensionLoader
import com.kaolinmc.tooling.api.environment.*
import com.kaolinmc.tooling.api.tweaker.EnvironmentTweaker

public class AppTweaker : EnvironmentTweaker {
    override fun tweak(
        environment: ExtensionEnvironment
    ) {
        // Target linker/resolver
        val linker = TargetLinker()

        linker.target = environment[ApplicationTarget]
        environment += linker

        val targetLinkerResolver = TargetLinkerResolver(linker)
        environment += targetLinkerResolver
        environment[ExtensionLoader].graph.resolvers.register(targetLinkerResolver)

        environment.find(internalExtraAppConfigAttrKey)?.value?.invoke(environment)
    }
}