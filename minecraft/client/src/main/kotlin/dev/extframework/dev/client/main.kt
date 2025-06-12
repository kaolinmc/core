@file:JvmName("Main")

package dev.extframework.dev.client

import BootLoggerFactory
import com.durganmcbroom.jobs.launch
import com.fasterxml.jackson.module.kotlin.readValue
import dev.extframework.boot.util.basicObjectMapper
import dev.extframework.common.util.resolve
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.minecraft.api.MappingNamespace
import dev.extframework.extloader.DefaultExtensionLoader
import dev.extframework.extloader.RootExtensionEnvironment
import dev.extframework.extloader.extension.DefaultExtensionResolver
import dev.extframework.extloader.extension.partition.DefaultPartitionResolver
import dev.extframework.minecraft.client.api.LaunchContext
import dev.extframework.minecraft.client.api.MinecraftExtensionInitializer
import dev.extframework.`object`.ObjectContainerImpl
import dev.extframework.tooling.api.environment.EnvironmentRegistry
import dev.extframework.tooling.api.environment.ValueAttribute
import dev.extframework.tooling.api.extension.artifact.ExtensionDescriptor
import dev.extframework.tooling.api.extension.artifact.ExtensionRepositorySettings
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path

public fun main(args: Array<String>) {
    val context = args.getOrNull(0) ?: throw Exception("Invalid arguments. First arg must be a path.")

    val launchContext = basicObjectMapper.readValue<LaunchContext>(context)

    launch(BootLoggerFactory()) {
        runBlocking {
            val archiveGraph = setupArchiveGraph(getHomedir() resolve "archives")
            addPackagedDependencies(archiveGraph, parsePackagedDependencies())
            val dependencyTypes = setupDependencyTypes(archiveGraph)

            val registry: EnvironmentRegistry = ObjectContainerImpl()

            val environment = RootExtensionEnvironment(
                "root",
                getHomedir(),
                dependencyTypes,
            )
            registry.register("root", environment)

            val classpathApp = ClasspathApp(
                launchContext.classpath,
                launchContext.version,
                getMinecraftDir(),
                launchContext.gameJar,
                launchContext.mainClass
            )

            environment += classpathApp

            environment += ValueAttribute(
                MappingNamespace.parse(launchContext.namespace),

                ValueAttribute.Key("mapping-target")
            )

            val loader = DefaultExtensionLoader(
                ClientExtensionResolver(
                    registry,
                    "root"
                ),
                archiveGraph,
                environment,
                registry,
            )

            loader.cache(
                mapOf(
                    ExtensionDescriptor.parseDescriptor(launchContext.targetExtension) to ExtensionRepositorySettings.local(
                        path = launchContext.repository,
                    )
                )
            )().merge()

            val loaded = loader.load(
                listOf(ExtensionDescriptor.parseDescriptor(launchContext.targetExtension))
            )().merge()

            loader.tweak(loaded, environment)().merge()

            environment[MinecraftExtensionInitializer].initialize(
                loaded
            )().merge()

            val app = environment[ApplicationTarget].node.handle!!.classloader

            val mainClass = app.loadClass(
                launchContext.mainClass
            )

            mainClass.getMethod("main", Array<String>::class.java)
                .invoke(null, launchContext.gameArguments.toTypedArray())
        }
    }
}

private class ClientExtensionResolver(
    environmentRegistry: EnvironmentRegistry, defaultEnvironment: String,
) : DefaultExtensionResolver(
    ClientExtensionResolver::class.java.classLoader, environmentRegistry, defaultEnvironment
) {
    override val partitionResolver: DefaultPartitionResolver = object : DefaultPartitionResolver(
        accessBridge,
        environmentRegistry,
        defaultEnvironment,
    ) {
//        override fun pathForDescriptor(descriptor: PartitionDescriptor, classifier: String, type: String): Path {
//            return path resolve super.pathForDescriptor(descriptor, classifier, type)
//        }
    }

//    override fun pathForDescriptor(descriptor: ExtensionDescriptor, classifier: String, type: String): Path {
//        return path resolve super.pathForDescriptor(descriptor, classifier, type)
//    }
}

private fun getHomedir(): Path {
    return getMinecraftDir() resolve ".extframework"
}

private fun getMinecraftDir(): Path {
    val osName = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")

    return when {
        osName.contains("win") -> {
            val appData = System.getenv("APPDATA")?.let(::Path) ?: Path(userHome, "AppData", "Roaming")
            appData resolve ".minecraft"
        }

        osName.contains("mac") -> Paths.get(userHome, "Library", "Application Support", "minecraft")
        else -> Paths.get(userHome, ".minecraft") // Assuming Linux/Unix-like
    }
}