package dev.extframework.core.main

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import dev.extframework.gradle.api.GradleEntrypoint
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import org.gradle.api.Project

public class MainGradlePlugin : GradleEntrypoint {
    override fun apply(project: Project) { }

    override fun setup(environment: ExtensionEnvironment): Job<Unit> = job { }
}