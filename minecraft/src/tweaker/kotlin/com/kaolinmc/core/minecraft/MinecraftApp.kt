package com.kaolinmc.core.minecraft

import com.kaolinmc.archive.mapper.ArchiveMapping
import com.kaolinmc.archive.mapper.findShortest
import com.kaolinmc.archive.mapper.newMappingsGraph
import com.kaolinmc.archive.mapper.transform.transformArchive
import com.kaolinmc.archives.ArchiveHandle
import com.kaolinmc.archives.ArchiveReference
import com.kaolinmc.archives.Archives
import com.kaolinmc.archives.zip.ZipFinder
import com.kaolinmc.archives.zip.classLoaderToArchive
import com.kaolinmc.boot.archive.ArchiveAccessTree
import com.kaolinmc.boot.archive.ClassLoadedArchiveNode
import com.kaolinmc.boot.getLogger
import com.kaolinmc.boot.loader.*
import com.kaolinmc.common.util.make
import com.kaolinmc.common.util.resolve
import com.kaolinmc.core.app.TargetLinker
import com.kaolinmc.core.app.api.ApplicationDescriptor
import com.kaolinmc.core.instrument.InstrumentedApplicationTarget
import com.kaolinmc.core.instrument.instrumentAgentsAttrKey
import com.kaolinmc.core.instrument.internal.InstrumentedAppImpl
import com.kaolinmc.core.minecraft.api.MinecraftAppApi
import com.kaolinmc.core.minecraft.environment.mappingProvidersAttrKey
import com.kaolinmc.core.minecraft.environment.mappingTargetAttrKey
import com.kaolinmc.core.minecraft.internal.MojangMappingProvider
import com.kaolinmc.core.minecraft.util.emptyArchiveHandle
import com.kaolinmc.core.minecraft.util.write
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment
import com.kaolinmc.tooling.api.environment.wrkDirAttrKey
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.jar.Manifest
import kotlin.io.path.*
import kotlin.reflect.KProperty

public fun MinecraftApp(
    instrumentedApp: InstrumentedApplicationTarget,
    environment: ExtensionEnvironment
): InstrumentedAppImpl {
    val delegate = instrumentedApp.delegate
    check(delegate is MinecraftAppApi) {
        "Invalid environment. The application target should be an instance of '${MinecraftAppApi::class.qualifiedName}'."
    }

    val mcApp = MinecraftApp(
        environment,
        delegate,
    )

    return InstrumentedAppImpl(
        mcApp,
        environment[TargetLinker],
        environment[instrumentAgentsAttrKey]
    )
}

public class MinecraftApp internal constructor(
    private val environment: ExtensionEnvironment,
    public val delegate: MinecraftAppApi,
) : MinecraftAppApi() {
    private val logger = getLogger()
    private val wrkDir = environment[wrkDirAttrKey]

    override val path: Path by delegate::path
    override val gameDir: Path by delegate::gameDir
    override val version: String by delegate::version
    // We specifically dont map this value
    override val mainClass: String = delegate.mainClass

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

    public fun setup() {
        if (setup) return

        val source = MojangMappingProvider.Companion.OBF_TYPE
        val destination = environment[mappingTargetAttrKey]

        val remappedPath: Path =
            wrkDir.value resolve "remapped" resolve "minecraft" resolve destination.value.path resolve delegate.node.descriptor.version
        val mappingsMarker = remappedPath resolve ".marker-v2"

        if (source == destination.value) {
            gameJar = delegate.gameJar
            classpath = delegate.classpath
            internalNode.delegateHandle = delegate.node.handle!!
            setup = true

            return
        }

        var gameJar = delegate.gameJar

        val classpath = if (!mappingsMarker.exists()) {
            val mappings: ArchiveMapping by lazy {
                newMappingsGraph(environment[mappingProvidersAttrKey].toList())
                    .findShortest(source.identifier, destination.value.identifier)
                    .forIdentifier(delegate.node.descriptor.version)
            }

            logger.info("Remapping Minecraft $version from: '$source' to '${destination.value}'. This may take a second.")
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
        class LateInit<T : Any>(
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

        fun <T : Any> lateInit(
            initialized: KProperty<Boolean>
        ) = LateInit<T>(initialized)
    }
}