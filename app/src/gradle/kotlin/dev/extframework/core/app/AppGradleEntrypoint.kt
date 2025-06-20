package dev.extframework.core.app

import dev.extframework.core.app.internal.internalExtraAppConfigAttrKey
import dev.extframework.gradle.api.BuildEnvironment
import dev.extframework.gradle.api.GradleEntrypoint
import dev.extframework.tooling.api.environment.ValueAttribute
import org.gradle.api.Project

public class AppGradleEntrypoint: GradleEntrypoint {
    override fun apply(project: Project) {}

    override fun tweak(root: BuildEnvironment) {
        root += ValueAttribute({
            root.extension.sourcesGraph.registerResolver(
                it[TargetLinkerResolver]
            )
        }, internalExtraAppConfigAttrKey)
    }
}