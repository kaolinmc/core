package dev.extframework.tests.core

import com.durganmcbroom.artifact.resolver.ArtifactMetadata
import dev.extframework.archives.ArchiveHandle
import dev.extframework.archives.Archives
import dev.extframework.boot.archive.*
import dev.extframework.boot.loader.*
import dev.extframework.core.app.api.ApplicationDescriptor
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.minecraft.api.MinecraftAppApi
import java.nio.file.Path
import kotlin.io.path.Path

//fun createEmptyApp(): ApplicationTarget {
//    return object : ApplicationTarget {
//        override val node: ClassLoadedArchiveNode<ApplicationDescriptor> =
//
//        override val path: Path = Path("")
//    }
//}

//fun createInstrumentedApp(
//    delegate: ApplicationTarget,
//): ApplicationTarget {
//    return object : InstrumentedApplicationTarget {
//        override val node: ClassLoadedArchiveNode<ApplicationDescriptor> =
//            delegate.node
//        override val path: Path = Path("")
//        override val delegate: ApplicationTarget = delegate
//        override val agents: MutableList<InstrumentAgent> = ArrayList()
//
//        override fun registerAgent(agent: InstrumentAgent) {
//            agents.add(agent)
//        }
//
//        override fun redefine(name: String) {
//
//        }
//    }
//}

public class App  : MinecraftAppApi() {
    override val node: ClassLoadedArchiveNode<ApplicationDescriptor> =
        object : ClassLoadedArchiveNode<ApplicationDescriptor> {
            private val appDesc = ApplicationDescriptor.parseDescription("test:app:1")!!
            override val access: ArchiveAccessTree = object : ArchiveAccessTree {
                override val descriptor: ArtifactMetadata.Descriptor = appDesc
                override val targets: List<ArchiveTarget> = listOf()
            }
            override val descriptor: ApplicationDescriptor = appDesc
            val archive = Archives.Finders.ZIP_FINDER.find(Path("tests/mc/target-app.jar"))
            override val handle: ArchiveHandle = Archives.resolve(
                archive,
                IntegratedLoader(
                    "App",
                    sourceProvider = ArchiveSourceProvider(archive),
                    resourceProvider = ArchiveResourceProvider(archive),
                    parent = ClassLoader.getSystemClassLoader(),
                ),
                Archives.Resolvers.ZIP_RESOLVER,
                setOf()
            ).archive
        }
    override val path: Path = Path("")

    override val gameDir: Path = Path("tests/mc")
    override val gameJar: Path = Path("tests/mc/target-app.jar")
    override val classpath: List<Path> = listOf()
    override var version: String = "1"
    override val mainClass: String = "doesnt.matter"
}

public fun createMinecraftApp(): App = App()

public fun createBlackboxApp(path: Path): ApplicationTarget {
    return object : ApplicationTarget {
        override val node: ClassLoadedArchiveNode<ApplicationDescriptor> =
            object : ClassLoadedArchiveNode<ApplicationDescriptor> {
                private val appDesc = ApplicationDescriptor.parseDescription("test:app:1")!!
                override val access: ArchiveAccessTree = object : ArchiveAccessTree {
                    override val descriptor: ArtifactMetadata.Descriptor = appDesc
                    override val targets: List<ArchiveTarget> = listOf()
                }
                override val descriptor: ApplicationDescriptor = appDesc
                override val handle: ArchiveHandle = run {
                    val ref = Archives.find(path, Archives.Finders.ZIP_FINDER)
                    Archives.resolve(
                        ref,
                        ArchiveClassLoader(ref, access, ClassLoader.getSystemClassLoader()),
                        Archives.Resolvers.ZIP_RESOLVER
                    ).archive
                }
            }
        override val path: Path = path
    }
}

//fun createMinecraftApp(
//    path: Path,
//    version: String,
//    archiveGraph: ArchiveGraph,
//    dependencyTypes: DependencyTypeContainer,
//): Job<ApplicationTarget> = job {
//    class AppTarget(
//        private val delegate: ClassLoader,
//        override val version: String,
//        override val path: Path,
//        access: ArchiveAccessTree,
//        override val classpath: List<Path>,
//        override val gameDir: Path,
//        override val gameJar: Path,
//    ) : MinecraftAppApi() {
//        override val node: ClassLoadedArchiveNode<ApplicationDescriptor> =
//            object : ClassLoadedArchiveNode<ApplicationDescriptor> {
//                private val appDesc = ApplicationDescriptor(
//                    "net.minecraft",
//                    "minecraft",
//                    version,
//                    "client"
//                )
//                override val descriptor: ApplicationDescriptor = appDesc
//                override val access: ArchiveAccessTree = access
//                override val handle: ArchiveHandle = classLoaderToArchive(
//                    MutableClassLoader(
//                        name = "minecraft-loader",
//                        resources = MutableResourceProvider(
//                            mutableListOf(
//                                object : ResourceProvider {
//                                    override fun findResources(name: String): Sequence<URL> {
//                                        return delegate.getResources(name).asSequence()
//                                    }
//                                }
//                            )),
//                        sources = object : MutableSourceProvider(
//                            mutableListOf(
//                                object : SourceProvider {
//                                    override val packages: Set<String> = setOf("*")
//
//                                    override fun findSource(name: String): ByteBuffer? {
//                                        val stream = delegate.getResourceAsStream(name.replace(".", "/") + ".class")
//                                            ?: return null
//
//                                        val bytes = stream.readInputStream()
//                                        return ByteBuffer.wrap(bytes)
//                                    }
//                                }
//                            )) {
//                            override fun findSource(name: String): ByteBuffer? =
//                                ((packageMap[name.substring(0, name.lastIndexOf('.').let { if (it == -1) 0 else it })]
//                                    ?: listOf()) +
//                                        (packageMap["*"] ?: listOf())).firstNotNullOfOrNull { it.findSource(name) }
//                        },
//                        parent = ClassLoader.getSystemClassLoader(),
//                    )
//                )
//            }
//    }
//
//    val node = dev.extframework.minecraft.bootstrapper.loadMinecraft(
//        version,
//        SimpleMavenRepositorySettings.local(), // SimpleMavenRepositorySettings.default(url = "https://maven.extframework.dev/snapshots"),
//        path,
//        archiveGraph,
//        dependencyTypes.get("simple-maven")!!.resolver as MavenLikeResolver<ClassLoadedArchiveNode<SimpleMavenDescriptor>, *>,
//        object : MinecraftProviderFinder {
//            override fun find(version: String): SimpleMavenDescriptor {
//                return SimpleMavenDescriptor.parseDescription("dev.extframework.minecraft:minecraft-provider-def:2.0.14-SNAPSHOT")!!
//            }
//        }
//    )().merge()
//
//    val loader = IntegratedLoader(
//        name = "Minecraft",
//        resourceProvider = MutableResourceProvider(
//            (node.libraries.map { it.archive } + node.archive)
//                .mapTo(ArrayList()) { ArchiveResourceProvider(it) }
//        ),
//        sourceProvider = MutableSourceProvider(
//            (node.libraries.map { it.archive } + node.archive)
//                .mapTo(ArrayList()) { ArchiveSourceProvider(it) }
//        ),
//        parent = ClassLoader.getSystemClassLoader(),
//    )
//
//    AppTarget(
//        loader,
//        version,
//        Paths.get(node.archive.location),
//        node.access,
//        listOf(node.archive.location.toPath()) + node.libraries.map { it.archive.location.toPath() },
//        path,
//        node.archive.location.toPath()
//    )
//}