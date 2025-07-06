package com.kaolinmc.core.app

import com.kaolinmc.core.app.internal.internalExtraAppConfigAttrKey
import com.kaolinmc.kiln.api.KaolinExtension
import com.kaolinmc.kiln.api.GradleEntrypoint
import com.kaolinmc.kiln.api.source.SourcesManager
import com.kaolinmc.tooling.api.environment.ValueAttribute

public class AppGradleEntrypoint : GradleEntrypoint {
    override suspend fun configure(
        extension: KaolinExtension,
        helper: GradleEntrypoint.Helper
    ) {
        extension.rootEnvironment += ValueAttribute(internalExtraAppConfigAttrKey) {
            it[SourcesManager].graph.resolvers.register(
                it[TargetLinkerResolver]
            )
        }
    }
}