package dev.extframework.core.instrument.internal

import dev.extframework.archives.ArchiveHandle
import dev.extframework.archives.transform.AwareClassWriter
import dev.extframework.archives.zip.classLoaderToArchive
import dev.extframework.boot.archive.ArchiveAccessTree
import dev.extframework.boot.archive.ClassLoadedArchiveNode
import dev.extframework.boot.util.toEnumeration
import dev.extframework.common.util.runCatching
import dev.extframework.core.app.TargetLinker
import dev.extframework.core.app.api.ApplicationDescriptor
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.instrument.InstrumentAgent
import dev.extframework.core.instrument.InstrumentedApplicationTarget
import dev.extframework.tooling.api.environment.MutableListAttribute
import net.bytebuddy.agent.ByteBuddyAgent
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.InputStream
import java.lang.instrument.ClassDefinition
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.writeBytes

// TODO environment composition
public class InstrumentedAppImpl(
    override val delegate: ApplicationTarget,
    linker: TargetLinker,
    override val agents: MutableListAttribute<InstrumentAgent>
) : InstrumentedApplicationTarget {
    private val instrumentation by lazy { ByteBuddyAgent.install() }
    private val instrumentedLoader = InstrumentedClassLoader(
        linker, agents, delegate.node.handle!!
    )

    //"https://api.modrinth.com/v2/project/AANobbMI/version?loaders=fabric&game_versions=1.21.4"

    override val node: ClassLoadedArchiveNode<ApplicationDescriptor> =
        object : ClassLoadedArchiveNode<ApplicationDescriptor> {
            override val access: ArchiveAccessTree = delegate.node.access
            override val descriptor: ApplicationDescriptor = delegate.node.descriptor

            override val handle: ArchiveHandle = classLoaderToArchive(instrumentedLoader)
        }

    override val path: Path by delegate::path

    override fun registerAgent(agent: InstrumentAgent) {
        agents.add(agent)
    }

    override fun redefine(name: String) {
        if (!instrumentedLoader.isClassLoaded(name)) return

        instrumentation.redefineClasses(
            ClassDefinition(
                node.handle!!.classloader.loadClass(name),
                instrumentedLoader.getClassBytes(name)
            )
        )
    }
}

private class InstrumentedClassLoader(
    val linker: TargetLinker,
    val agents: MutableListAttribute<InstrumentAgent>,
    val delegate: ArchiveHandle,
) : ClassLoader(linker.extensionLoader) {
    private fun InputStream.classNode(parsingOptions: Int = ClassReader.EXPAND_FRAMES): ClassNode {
        val node = ClassNode()
        ClassReader(this).accept(node, parsingOptions)
        return node
    }

    override fun findClass(name: String): Class<*>? {
        val bytes = getClassBytes(name) ?: return null

        return defineClass(name, bytes, 0, bytes.size)
    }

    override fun findResources(name: String): Enumeration<URL> {
        return sequenceOf(findResource(name))
            .filterNotNull()
            .toEnumeration()
    }

    override fun findResource(name: String): URL? {
        if (name.endsWith(".class")) {
            val clsName = name.removeSuffix(".class").replace("/", ".")

            val clsBytes = getClassBytes(clsName)

            if (clsBytes != null) {
                val tmp = Files.createTempFile(clsName, null)

                tmp.writeBytes(clsBytes)

                return tmp.toUri().toURL()
            }
        }

        return delegate.classloader.getResource(name)
    }

    override fun toString(): String {
        return "Mixins @ app"
    }

    fun isClassLoaded(name: String): Boolean {
        return findLoadedClass(name) != null
    }

    fun getClassBytes(name: String): ByteArray? {
        val reader = delegate.getResource(name.replace('.', '/') + ".class")
            ?.let { resource -> ClassReader(resource) }

        val node = reader?.let {
            val node = ClassNode()
            reader.accept(node, ClassReader.EXPAND_FRAMES)
            node
        }

        val transformedNode = try {
            // TODO merging
            agents.fold(node) { acc, it ->
                it.transformClass(name, acc)
            } ?: return null
        } catch (e: Throwable) {
            e.printStackTrace()
            throw e
        }

        val writer = object : AwareClassWriter(
            listOf(),
            COMPUTE_FRAMES,
            reader
        ) {
            override fun loadType(name: String): HierarchyNode {
                val resourceName = "$name.class"
                val resource =
                    this@InstrumentedClassLoader.delegate.classloader.getResource(
                        resourceName
                    ) ?: linker.extensionLoader.getResource(
                        resourceName
                    )

                return resource?.openStream()?.classNode()
                    ?.let(::UnloadedClassNode)
                    ?: runCatching<HierarchyNode>(ClassNotFoundException::class) {
                        LoadedClassNode(Class.forName(name.replace('/', '.')))
                    } ?: run {
                        System.err.println("Recomputing stack frames and asked for super type information about class: '$name'. This class could not be loaded so a stub (inheriting from java/lang/Object) was returned.")
                        // This is confusing. The only really that we do this is because it's better to return a type than not.
                        // Often a class not being found in this stage is more an issue with the app we are targeting than anything
                        // the user can control. We set isInterface to true so that java/lang/Object is returned.
                        object : HierarchyNode {
                            override val interfaceNodes: List<HierarchyNode> = listOf()
                            override val isInterface: Boolean = true
                            override val name: String = name
                            override val superNode: HierarchyNode? = null
                        }
                    }
            }
        }

        transformedNode.accept(writer)

        return writer.toByteArray()
    }

    companion object {
        init {
            registerAsParallelCapable()
        }
    }
}

