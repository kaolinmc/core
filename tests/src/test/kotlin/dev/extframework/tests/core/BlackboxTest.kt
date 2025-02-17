package dev.extframework.tests.core

import BootLoggerFactory
import com.durganmcbroom.jobs.launch
import dev.extframework.extloader.InternalExtensionEnvironment
import dev.extframework.extloader.initExtensions
import dev.extframework.tooling.api.extension.artifact.ExtensionDescriptor
import dev.extframework.tooling.api.extension.artifact.ExtensionRepositorySettings
import kotlinx.coroutines.runBlocking
import kotlin.io.path.Path
import kotlin.test.Test

class BlackboxTest {
    @Test
    fun `Test extension run`() {
        val path = Path("tests", "test-extension-run").toAbsolutePath()

        launch(BootLoggerFactory()) {
            runBlocking {
                val (graph, types) = setupBoot(path)

                initExtensions(
                    mapOf(
                        ExtensionDescriptor.parseDescriptor(
                            "com.example:blackbox:1"
                        ) to ExtensionRepositorySettings.local()
                    ),
                    InternalExtensionEnvironment(
                        path, graph, types,
                    ).also {
                        it += createMinecraftApp()
                    }
                )().merge()
            }
        }
    }
}