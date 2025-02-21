package dev.extframework.core.minecraft

import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import com.durganmcbroom.jobs.logging.info
import dev.extframework.archive.mapper.ArchiveMapping
import dev.extframework.archive.mapper.findShortest
import dev.extframework.archive.mapper.newMappingsGraph
import dev.extframework.archive.mapper.transform.transformArchive
import dev.extframework.archives.ArchiveHandle
import dev.extframework.archives.ArchiveReference
import dev.extframework.archives.Archives
import dev.extframework.archives.zip.ZipFinder
import dev.extframework.archives.zip.classLoaderToArchive
import dev.extframework.boot.archive.ArchiveAccessTree
import dev.extframework.boot.archive.ClassLoadedArchiveNode
import dev.extframework.boot.loader.*
import dev.extframework.common.util.make
import dev.extframework.common.util.resolve
import dev.extframework.core.app.TargetLinker
import dev.extframework.core.app.api.ApplicationDescriptor
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.instrument.InstrumentedApplicationTarget
import dev.extframework.core.instrument.internal.InstrumentedAppImpl
import dev.extframework.core.instrument.instrumentAgentsAttrKey
import dev.extframework.core.minecraft.api.MinecraftAppApi
import dev.extframework.core.minecraft.environment.mappingProvidersAttrKey
import dev.extframework.core.minecraft.environment.mappingTargetAttrKey
import dev.extframework.core.minecraft.internal.MojangMappingProvider
import dev.extframework.core.minecraft.util.write
import dev.extframework.core.minecraft.util.emptyArchiveHandle
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.environment.extract
import dev.extframework.tooling.api.environment.wrkDirAttrKey
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.jar.Manifest
import kotlin.io.path.*
import kotlin.reflect.KProperty

internal fun MinecraftApp(
    instrumentedApp: InstrumentedApplicationTarget,
    environment: ExtensionEnvironment
): Job<InstrumentedAppImpl> = job {
    val delegate = instrumentedApp.delegate
    check(delegate is MinecraftAppApi) {
        "Invalid environment. The application target should be an instance of '${MinecraftAppApi::class.qualifiedName}'."
    }

    val mcApp = MinecraftApp(
        environment,
        delegate,
    )

    InstrumentedAppImpl(
        mcApp,
        environment[TargetLinker].extract(),
        environment[instrumentAgentsAttrKey].extract()
    )
}

public class MinecraftApp internal constructor(
    private val environment: ExtensionEnvironment,
    public val delegate: MinecraftAppApi,
) : MinecraftAppApi() {
    private val wrkDir by environment[wrkDirAttrKey]

    override val path: Path by delegate::path
    override val gameDir: Path by delegate::gameDir
    override val version: String by delegate::version

    override var gameJar: Path by lateInit(::setup)
        private set
    override var classpath: List<Path> by lateInit(::setup)
        private set

    private inner class InternalNode : ClassLoadedArchiveNode<ApplicationDescriptor> {
        var delegateHandle: ArchiveHandle = emptyArchiveHandle()

        override val handle: ArchiveHandle = object : ArchiveHandle {
            override val classloader: ClassLoader
                get() = delegateHandle.classloader
            override val name: String?
                get() = delegateHandle.name
            override val packages: Set<String>
                get() = delegateHandle.packages
            override val parents: Set<ArchiveHandle>
                get() = delegateHandle.parents
        }
        override var access: ArchiveAccessTree = delegate.node.access
        override var descriptor: ApplicationDescriptor = delegate.node.descriptor
    }
    private val internalNode = InternalNode()
    override val node: ClassLoadedArchiveNode<ApplicationDescriptor> = internalNode

    public var setup: Boolean = false
        private set

    internal fun setup() = job {
        if (setup) return@job

        val source = MojangMappingProvider.Companion.OBF_TYPE
        val destination by environment[mappingTargetAttrKey]

        val remappedPath: Path =
            wrkDir.value resolve "remapped" resolve "minecraft" resolve destination.value.path resolve delegate.node.descriptor.version
        val mappingsMarker = remappedPath resolve ".marker-v2"

        if (source == destination.value) {
            gameJar = delegate.gameJar
            classpath = delegate.classpath
            internalNode.delegateHandle = delegate.node.handle!!
            setup = true

            return@job
        }

        var gameJar = delegate.gameJar

        val classpath = if (!mappingsMarker.exists()) {
            val mappings: ArchiveMapping by lazy {
                newMappingsGraph(environment[mappingProvidersAttrKey].extract())
                    .findShortest(source.identifier, destination.value.identifier)
                    .forIdentifier(delegate.node.descriptor.version)
            }

            info("Remapping Minecraft from: '$source' to '${destination.value}'. This may take a second.")
            val remappedJars = delegate.classpath.mapNotNull { t ->
                val name = UUID.randomUUID().toString() + ".jar"
                val isGame = t == delegate.gameJar

                if (t.extension != "jar") {
                    System.err.println(
                        "Found minecraft file on classpath: '$t' but it is not a jar. Will not remap it."
                    )
                    return@mapNotNull null
                }

                Archives.find(t, ZipFinder).use { archive ->
                    transformArchive(
                        archive,
                        listOf(delegate.node.handle!!),
                        mappings,
                        source.identifier,
                        destination.value.identifier,
                    )

                    val manifest = archive.getResource("META-INF/MANIFEST.MF")

                    if (manifest != null) {
                        val manifest = Manifest()

                        // Stripping checksums
                        stripManifestChecksums(
                            manifest,
                        )
                        val bytes = ByteArrayOutputStream().use {
                            manifest.write(it)
                            it
                        }.toByteArray()

                        archive.writer.put(
                            ArchiveReference.Entry(
                                "META-INF/MANIFEST.MF",
                                false,
                                archive
                            ) {
                                ByteArrayInputStream(bytes)
                            })
                    }

                    archive.reader
                        .entries()
                        .filter { it.name.startsWith("META-INF/") }
                        .forEach { entry ->
                            if (isSigningRelated(entry.name)) {
                                archive.writer.remove(entry.name)
                            }
                        }

                    // Write out
                    val path = remappedPath resolve name

                    path.make()
                    archive.write(path)

                    if (isGame) {
                        gameJar = path
                    }

                    path to (if (isGame) "game" else "lib")
                }
            }

            mappingsMarker.make()
            mappingsMarker.writeLines(remappedJars.map { "${it.second}${File.pathSeparator}${it.first}" })

            remappedJars.map { it.first }
        } else {
            mappingsMarker.readLines().map {
                val (type, path) = it.split(File.pathSeparator)
                val pathObj = Path(path)

                if (type == "game") {
                    gameJar = pathObj
                }

                pathObj
            }
        }

        val references = classpath.map { it -> Archives.find(it, ZipFinder) }

        val sources = DelegatingSourceProvider(
            references.map(::ArchiveSourceProvider)
        )

        val classLoader = IntegratedLoader(
            "Minecraft",
            sourceProvider = sources,
            resourceProvider = DelegatingResourceProvider(references.map(::ArchiveResourceProvider)),
            // TODO platform class loader?
            parent = delegate.node.handle?.classloader?.parent ?: ClassLoader.getSystemClassLoader()
        )

        this@MinecraftApp.gameJar = gameJar
        this@MinecraftApp.classpath = classpath
        this@MinecraftApp.internalNode.delegateHandle = classLoaderToArchive(classLoader)
        setup = true
    }

    private fun stripManifestChecksums(
        manifest: Manifest
    ) {
        manifest.entries.clear()
    }

    // @see java.util.jar.JarVerifier#isSigningRelated
    private fun isSigningRelated(
        name: String
    ): Boolean {
        return name.endsWith(".SF")
                || name.endsWith(".DSA")
                || name.endsWith(".RSA")
                || name.endsWith(".EC")
                || name.startsWith("SIG-")
    }

    private companion object {
        class LateInit<T: Any>(
            private val initialized: KProperty<Boolean>
        ) {
            private var value: T? = null

            operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
                if (!initialized.getter.call()) {
                    throw Exception("You cannot yet consume: '${property.name}'. The application target Minecraft has not been initialized yet, please call #setup (or wait for it to be called) before you consume this value.")
                }

                return value!!
            }

            operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T): Unit {
                this.value = value
            }
        }

        fun <T: Any> lateInit(
            initialized: KProperty<Boolean>
        ) = LateInit<T>(initialized)
    }
}