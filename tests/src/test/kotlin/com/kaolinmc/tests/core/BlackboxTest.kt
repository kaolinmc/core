package com.kaolinmc.tests.core

import com.durganmcbroom.resources.KtorInstance
import com.kaolinmc.core.app.api.ApplicationTarget
import com.kaolinmc.minecraft.client.api.MinecraftExtensionInitializer
import com.kaolinmc.minecraft.client.api.minecraftInitializersAttrKey
import com.kaolinmc.tooling.api.extension.artifact.ExtensionDescriptor
import com.kaolinmc.tooling.api.extension.artifact.ExtensionRepositorySettings
import io.ktor.client.request.post
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.test.Test

public class BlackboxTest {
    @Test
    public fun `Test extension run`() {
        val path = Path("tests", "test-extension-run").toAbsolutePath()

        val (loader) = newLoader()

        loader.environment += createMinecraftApp()

        runBlocking {
            val descriptor = ExtensionDescriptor.parseDescriptor(
                "com.example:blackbox:1"
            )
            loader.cache(
                mapOf(
                    descriptor to ExtensionRepositorySettings.local()
                )
            )

            val loaded = loader.load(listOf(descriptor))

            loader.tweak(loaded)
//            loader.runInitialization()().merge()
        }
    }

    @Test
    public fun `Test extension reloading`() {

        val path = Path("tests", "test-extension-reload").toAbsolutePath()

        runBlocking {
            val (loader) = newLoader(path)

            val createMinecraftApp = createMinecraftApp()
            loader.environment += createMinecraftApp

            val descriptor = ExtensionDescriptor.parseDescriptor(
                "com.example:reload:1"
            )

            loader.cache(
                mapOf(
                    descriptor to ExtensionRepositorySettings.local()
                )
            )

            val extensions = loader.load(listOf(descriptor))

            loader.tweak(extensions)

            loader.environment[MinecraftExtensionInitializer].initialize(extensions)

            val cls = loader.environment[ApplicationTarget].node.handle!!.classloader.loadClass("targetapp.Main")
            val instance = cls.getConstructor().newInstance()
            val method = cls.getMethod("main")

            check(method.invoke(instance) as Int == 6)

            loader.unload(descriptor)

            check(method.invoke(instance) as Int == 5)

            createMinecraftApp.version = "2"

            loader.load(listOf(descriptor))
            loader.environment[MinecraftExtensionInitializer].initialize(extensions)

            check(method.invoke(instance) as Int == 7)
        }
    }
}