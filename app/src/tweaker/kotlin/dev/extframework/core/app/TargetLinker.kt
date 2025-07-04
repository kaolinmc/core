package dev.extframework.core.app

import dev.extframework.boot.loader.*
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.extension.artifact.ExtensionDescriptor
import java.net.URL
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// TODO: This currently stops currently loaded extension from being unloaded
//       as their classes are referenced. Two changes need to occur: Mutable
//       class provider should also have the ability to remove classes,
//       and one should be able to remove extension classes by referencing its
//       descriptor or something equivalent.
public class TargetLinker : ExtensionEnvironment.Attribute {
    override val key: ExtensionEnvironment.Attribute.Key<*> = TargetLinker

    private var clState: MutableSet<String> = HashSet()
    private var rlState: LinkerState = LinkerState.NEITHER
    private val clLock: ReentrantLock = ReentrantLock()
    private val rlLock: ReentrantLock = ReentrantLock()

    public lateinit var target: ApplicationTarget

    public val targetLoader: IntegratedLoader = IntegratedLoader(
        name = "Extension -> (Linker) -> App",
        classProvider = object : ClassProvider {
            override val packages: Set<String> by lazy {
                target.node.handle!!.packages
            }

            override fun findClass(name: String): Class<*>? {
                return findClassInternal(name, LinkerState.LOAD_TARGET)
            }
        },
        resourceProvider = object : ResourceProvider {
            override fun findResources(name: String): Sequence<URL> {
                return findResourceInternal(name, LinkerState.LOAD_TARGET)
            }
        },

        // TODO replacement for platform class loader?
        parent = ClassLoader.getSystemClassLoader(),
    )

    public val extensionClasses: MutableMap<ExtensionDescriptor, ClassProvider> =
        HashMap()// : MutableClassProvider = MutableClassProvider(ArrayList())
    public val extensionResources: MutableMap<ExtensionDescriptor, ResourceProvider> =
        HashMap() //: MutableResourceProvider = MutableResourceProvider(ArrayList())

    public val extensionLoader: ClassLoader = IntegratedLoader(
        name = "App -> (Linker) -> Extension",
        classProvider = object : ClassProvider {
            override val packages: Set<String>
                get() = extensionClasses.values.flatMapTo(HashSet()) { it.packages }

            override fun findClass(name: String): Class<*>? {
                return findClassInternal(name, LinkerState.LOAD_EXTENSION)
            }
        },
        resourceProvider = object : ResourceProvider {
            override fun findResources(name: String): Sequence<URL> {
                return findResourceInternal(name, LinkerState.LOAD_EXTENSION)
            }
        },
        parent = TargetLinker::class.java.classLoader,
    )

//    public fun addExtensionClasses(
//        extension: ExtensionDescriptor,
//        provider: ClassProvider
//    ) {
//        extensionClasses[extension] = provider
//    }//.add(provider)
//
//    public fun addExtensionResources(
//        extension: ExtensionDescriptor,
//        provider: ResourceProvider
//    ) {
//        extensionResources[extension] = provider
//    }// = extensionResources.add(provider)

    public fun findResources(name: String): Sequence<URL> {
        return findResourceInternal(name, LinkerState.LOAD_TARGET) +
                findResourceInternal(name, LinkerState.LOAD_EXTENSION)
    }

    // Dont need the same approach as class loading because loading one resource should never trigger the loading of another.
    private fun findResourceInternal(name: String, state: LinkerState): Sequence<URL> {
        return rlLock.withLock {
            if (
                (this.rlState == LinkerState.LOAD_TARGET && state == LinkerState.LOAD_EXTENSION) ||
                (this.rlState == LinkerState.LOAD_EXTENSION && state == LinkerState.LOAD_TARGET)
            ) {
                this.rlState = LinkerState.NEITHER
                return emptySequence()
            }

            this.rlState = state
            val r = when (state) {
                LinkerState.LOAD_TARGET -> target.node.handle!!.classloader.getResources(name).asSequence()
                LinkerState.LOAD_EXTENSION -> extensionResources.values.firstNotNullOfOrNull { it.findResources(name) }
                    ?: sequenceOf()

                LinkerState.NEITHER -> throw IllegalArgumentException("Cannot load linker state of neither.")
            }

            this.rlState = LinkerState.NEITHER

            r
        }
    }

    public fun findClass(name: String): Class<*>? {
        return findClassInternal(name, LinkerState.LOAD_TARGET)
            ?: findClassInternal(name, LinkerState.LOAD_EXTENSION)
    }

    private fun findClassInternal(name: String, state: LinkerState): Class<*>? {
        return clLock.withLock {
            if (
                clState.contains(name)
            ) {
                this.clState.clear()
                throw ClassNotFoundException()
            }

            this.clState.add(name)

            val c = when (state) {
                LinkerState.LOAD_TARGET -> target.node.handle!!.classloader.loadClass(name)
                LinkerState.LOAD_EXTENSION -> extensionClasses.values.firstNotNullOfOrNull { it.findClass(name) }
                LinkerState.NEITHER -> throw IllegalArgumentException("Cannot load linker state of 'NEITHER'.")
            }

            this.clState.remove(name)

            c
        }
    }

    public companion object : ExtensionEnvironment.Attribute.Key<TargetLinker>

    private enum class LinkerState {
        NEITHER,
        LOAD_TARGET,
        LOAD_EXTENSION
    }
}