@file:JvmName("Main")

package dev.extframework.dev.client

import com.fasterxml.jackson.module.kotlin.readValue
import dev.extframework.boot.util.basicObjectMapper
import dev.extframework.common.util.resolve
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.minecraft.api.MappingNamespace
import dev.extframework.extloader.DefaultExtensionEnvironment
import dev.extframework.extloader.DefaultExtensionLoader
import dev.extframework.extloader.extension.DefaultExtensionResolver
import dev.extframework.extloader.extension.partition.TweakerPartitionLoader
import dev.extframework.minecraft.client.api.LaunchContext
import dev.extframework.minecraft.client.api.MinecraftExtensionInitializer
import dev.extframework.tooling.api.environment.*
import dev.extframework.tooling.api.extension.artifact.ExtensionDescriptor
import dev.extframework.tooling.api.extension.artifact.ExtensionRepositorySettings
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path

public fun main(args: Array<String>) {
    val context = args.getOrNull(0) ?: throw Exception("Invalid arguments. First arg must be a path.")

    val launchContext = basicObjectMapper.readValue<LaunchContext>(context)

    runBlocking {
        val archiveGraph = setupArchiveGraph(getHomedir() resolve "archives")
        addPackagedDependencies(archiveGraph, parsePackagedDependencies())
        val dependencyTypes = setupDependencyTypes(archiveGraph)

        val environment = DefaultExtensionEnvironment(
            "root",
        )

        environment += ValueAttribute(wrkDirAttrKey, getHomedir())
        environment += ObjectContainerAttribute(dependencyTypesAttrKey, dependencyTypes)
        environment += ValueAttribute(parentCLAttrKey, ClassLoader.getSystemClassLoader())
        environment += ObjectContainerAttribute(partitionLoadersAttrKey).also { c ->
            TweakerPartitionLoader().also { c.container.register( it) }
        }

        val classpathApp = ClasspathApp(
            launchContext.classpath,
            launchContext.version,
            getMinecraftDir(),
            launchContext.gameJar,
            launchContext.mainClass
        )

        environment += classpathApp

        environment += ValueAttribute(
            ValueAttribute.Key("mapping-target"),
            MappingNamespace.parse(launchContext.namespace),
        )

        val loader = DefaultExtensionLoader(
            DefaultExtensionResolver(
                ClassLoader.getSystemClassLoader(),
                environment
            ),
            archiveGraph,
        )
        environment += loader

        loader.cache(
            mapOf(
                ExtensionDescriptor.parseDescriptor(launchContext.targetExtension) to ExtensionRepositorySettings.local(
                    path = launchContext.repository,
                )
            )
        )

        val loaded = loader.load(
            listOf(ExtensionDescriptor.parseDescriptor(launchContext.targetExtension))
        )

        loader.tweak(loaded, environment)

        environment[MinecraftExtensionInitializer].initialize(
            loaded
        )

        val app = environment[ApplicationTarget].node.handle!!.classloader

        val mainClass = app.loadClass(
            launchContext.mainClass
        )

        mainClass.getMethod("main", Array<String>::class.java)
            .invoke(null, launchContext.gameArguments.toTypedArray())
    }
}

//private class ClientExtensionResolver(
//) : DefaultExtensionResolver(
//    ClientExtensionResolver::class.java.classLoader, environmentRegistry, defaultEnvironment
//) {
//    override val partitionResolver: DefaultPartitionResolver = object : DefaultPartitionResolver(
//        accessBridge,
//        environmentRegistry,
//        defaultEnvironment,
//    ) {
////        override fun pathForDescriptor(descriptor: PartitionDescriptor, classifier: String, type: String): Path {
////            return path resolve super.pathForDescriptor(descriptor, classifier, type)
////        }
//    }
//
////    override fun pathForDescriptor(descriptor: ExtensionDescriptor, classifier: String, type: String): Path {
////        return path resolve super.pathForDescriptor(descriptor, classifier, type)
////    }
//}


//internal open class GradleExtensionResolver(
//    val isMocked: (ExtensionDescriptor) -> Boolean,
//    classloader: ClassLoader,
//    environment: ExtensionEnvironment,
////    environmentRegistry: EnvironmentRegistry,
////    defaultEnvironment: String,
//) : DefaultExtensionResolver(
//    classloader, environment
//) {
//    private val tempDir = Files.createTempDirectory("mocked-extensions")
//
//    override val partitionResolver: DefaultPartitionResolver = object : DefaultPartitionResolver(
//        accessBridge, environment//environmentRegistry, defaultEnvironment
//    ) {
//        override fun pathForDescriptor(descriptor: PartitionDescriptor, classifier: String, type: String): Path {
//            val basePath = if (isMocked(descriptor.extension)) {
//                tempDir
//            } else Path("extensions")
//
//            return basePath resolve super.pathForDescriptor(descriptor, classifier, type)
//        }
//    }
//
//    override fun pathForDescriptor(descriptor: ExtensionDescriptor, classifier: String, type: String): Path {
//        val basePath = if (isMocked(descriptor)) {
//            tempDir
//        } else Path("extensions")
//
//        return basePath resolve super.pathForDescriptor(descriptor, classifier, type)
//    }
//
//    internal class View(
//        private val _reference: () -> GradleExtensionResolver,
//        environment: ExtensionEnvironment
//    ) : GradleExtensionResolver(
//        _reference().isMocked,
//        ClassLoader.getSystemClassLoader(),
//        environment
//    ) {
//        private val reference: ExtensionResolver
//            get() = _reference()
//        override val layerLoader: ExtensionLayerClassLoader = ExtensionLayerClassLoader(
//            reference.layerLoader,
//            "Extension Layer ${environment.name}"
//        )
//
//        override val accessBridge: ExtensionResolver.AccessBridge = object : ExtensionResolver.AccessBridge {
//            override fun classLoaderFor(descriptor: ExtensionDescriptor): ExtensionClassLoader {
//                return (extensionClassloaders[descriptor.toIdentifier()]) ?: reference.accessBridge.classLoaderFor(
//                    descriptor
//                )
//            }
//
//            override fun ermFor(descriptor: ExtensionDescriptor): ExtensionRuntimeModel {
//                return extensionMetadata[descriptor.toIdentifier()]?.erm ?: reference.accessBridge.ermFor(descriptor)
//            }
//
//            override fun repositoryFor(descriptor: ExtensionDescriptor): ExtensionRepositorySettings {
//                return extensionMetadata[descriptor.toIdentifier()]?.repository ?: reference.accessBridge.repositoryFor(
//                    descriptor
//                )
//            }
//        }
//    }
//}

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