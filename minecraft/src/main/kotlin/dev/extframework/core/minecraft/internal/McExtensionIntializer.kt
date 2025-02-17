package dev.extframework.core.minecraft.internal

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import dev.extframework.core.minecraft.MinecraftApp
import dev.extframework.tooling.api.extension.ExtensionNode
import dev.extframework.tooling.api.extension.ExtensionInitializer

public class McExtensionIntializer(
    private val delegate: ExtensionInitializer?,
    private val app: MinecraftApp,
) : ExtensionInitializer {
    override fun init(node: ExtensionNode): Job<Unit> = job {
        app.setup()().merge()
        delegate?.init(node)?.invoke()?.merge()
    }
}