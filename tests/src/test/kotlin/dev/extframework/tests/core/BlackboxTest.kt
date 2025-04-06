package dev.extframework.tests.core

import BootLoggerFactory
import com.durganmcbroom.jobs.launch
import dev.extframework.extloader.DefaultExtensionLoader
import dev.extframework.extloader.InternalExtensionEnvironment
import dev.extframework.tooling.api.extension.artifact.ExtensionDescriptor
import dev.extframework.tooling.api.extension.artifact.ExtensionRepositorySettings
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.test.Test

public class BlackboxTest {
    @Test
    public fun `Test extension run`() {
        val path = Path("tests", "test-extension-run").toAbsolutePath()

        launch(BootLoggerFactory()) {
            val (graph, types) = setupBoot(path)

            val environment = InternalExtensionEnvironment(
                path, graph, types,
            ).also {
                it += createMinecraftApp()
            }

            val loader = DefaultExtensionLoader(environment)

            runBlocking {
                var descriptor = ExtensionDescriptor.parseDescriptor(
                    "com.example:blackbox:1"
                )
                loader.cache(
                    mapOf(
                        descriptor to ExtensionRepositorySettings.local()
                    )
                )().merge()

                loader.load(listOf(descriptor))().merge()

                loader.runTweakers()().merge()
                loader.runInitialization()().merge()
            }
        }
    }

    @Test
    public fun `Test extension reloading`() {
        val path = Path("tests", "test-extension-run").toAbsolutePath()

        launch(BootLoggerFactory()) {
            val (graph, types) = setupBoot(path)

            var createMinecraftApp = createMinecraftApp()
            val environment = InternalExtensionEnvironment(
                path, graph, types,
            ).also {
                it += createMinecraftApp
            }

            val loader = DefaultExtensionLoader(environment)

            runBlocking {
                var descriptor = ExtensionDescriptor.parseDescriptor(
                    "com.example:blackbox:1"
                )

                loader.cache(
                    mapOf(
                        descriptor to ExtensionRepositorySettings.local()
                    )
                )().merge()

                loader.load(listOf(descriptor))().merge()

                loader.runTweakers()().merge()
                loader.runInitialization()().merge()

                loader.unload(descriptor)().merge()

                createMinecraftApp.version = "2"

                loader.load(listOf(descriptor))().merge()

                loader.runInitialization()().merge()
                System.gc()

                println("")
            }
        }
    }
}