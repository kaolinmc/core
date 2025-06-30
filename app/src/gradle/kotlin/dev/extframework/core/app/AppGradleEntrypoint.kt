package dev.extframework.core.app

import dev.extframework.core.app.internal.internalExtraAppConfigAttrKey
import dev.extframework.gradle.api.ExtframeworkExtension
import dev.extframework.gradle.api.GradleEntrypoint
import dev.extframework.gradle.api.source.SourcesManager
import dev.extframework.tooling.api.environment.ValueAttribute

public class AppGradleEntrypoint : GradleEntrypoint {
    override suspend fun configure(
        extension: ExtframeworkExtension,
        helper: GradleEntrypoint.Helper
    ) {
        extension.rootEnvironment += ValueAttribute(internalExtraAppConfigAttrKey, {
            it[SourcesManager].graph.resolvers.register(
                it[TargetLinkerResolver]
            )
        })
    }
}