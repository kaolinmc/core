package dev.extframework.core.app

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.tooling.api.ExtensionLoader
import dev.extframework.tooling.api.environment.*
import dev.extframework.tooling.api.tweaker.EnvironmentTweaker

public class AppTweaker : EnvironmentTweaker {
    override fun tweak(
        environment: ExtensionEnvironment
    ): Job<Unit> = job {
        // Target linker/resolver
        val linker = TargetLinker()

        linker.target = environment[ApplicationTarget]
        environment += linker

        val targetLinkerResolver = TargetLinkerResolver(linker)
        environment += targetLinkerResolver
        environment[ExtensionLoader].graph.registerResolver(targetLinkerResolver)
    }
}