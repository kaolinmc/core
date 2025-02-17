package dev.extframework.core.instrument.internal

import dev.extframework.archives.ArchiveHandle
import dev.extframework.archives.transform.AwareClassWriter
import dev.extframework.archives.zip.classLoaderToArchive
import dev.extframework.boot.archive.ArchiveAccessTree
import dev.extframework.boot.archive.ClassLoadedArchiveNode
import dev.extframework.common.util.make
import dev.extframework.common.util.runCatching
import dev.extframework.core.app.TargetLinker
import dev.extframework.core.app.api.ApplicationDescriptor
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.instrument.InstrumentAgent
import dev.extframework.core.instrument.InstrumentedApplicationTarget
import dev.extframework.core.instrument.internal.InstrumentedAppImpl
import dev.extframework.tooling.api.environment.MutableObjectSetAttribute
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.InputStream
import java.net.URL
import java.nio.file.Path
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.writeBytes

public class InstrumentedAppImpl(
    override val delegate: ApplicationTarget,
    private val linker: TargetLinker,
    override val agents: MutableObjectSetAttribute<InstrumentAgent>
) : InstrumentedApplicationTarget {
    override val node: ClassLoadedArchiveNode<ApplicationDescriptor> =
        object : ClassLoadedArchiveNode<ApplicationDescriptor> {
            override val access: ArchiveAccessTree = delegate.node.access
            override val descriptor: ApplicationDescriptor = delegate.node.descriptor
            override val handle: ArchiveHandle = classLoaderToArchive(
                InstrumentedClassLoader(
                    linker, agents, delegate.node.handle!!
                )
            )
        }

    override val path: Path by delegate::path

    override fun registerAgent(agent: InstrumentAgent) {
        agents.add(agent)
    }

    override fun redefine(name: String) {
        TODO("Not yet implemented")
    }
}

private class InstrumentedClassLoader(
    val linker: TargetLinker,
    val agents: MutableObjectSetAttribute<InstrumentAgent>,
    val delegate: ArchiveHandle,
) : ClassLoader(linker.extensionLoader) {
    private fun InputStream.classNode(parsingOptions: Int = ClassReader.EXPAND_FRAMES): ClassNode {
        val node = ClassNode()
        ClassReader(this).accept(node, parsingOptions)
        return node
    }

    override fun findClass(name: String): Class<*>? {

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

        val bytes = writer.toByteArray()

        val path = Path("mixin-output/" + name.replace('.', '/') + ".class")
        path.make()
        path.writeBytes(bytes)

        return defineClass(name, bytes, 0, bytes.size)
    }

    override fun findResources(name: String): Enumeration<URL> {
        return delegate.classloader.getResources(name)
    }

    override fun findResource(name: String): URL? =
        delegate.classloader.getResource(name)

    override fun toString(): String {
        return "Mixins @ app"
    }

    companion object {
        init {
            registerAsParallelCapable()
        }
    }
}