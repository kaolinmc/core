package com.kaolinmc.core.minecraft.partition

//import com.kaolinmc.extension.core.feature.FeatureReference
//import com.kaolinmc.extension.core.feature.findImplementedFeatures
//import com.kaolinmc.extension.core.feature.implementsFeatures
import com.kaolinmc.archive.mapper.transform.ClassInheritancePath
import com.kaolinmc.archive.mapper.transform.ClassInheritanceTree
import com.kaolinmc.archive.mapper.transform.mapClassName
import com.kaolinmc.archives.ArchiveHandle
import com.kaolinmc.archives.ArchiveReference
import com.kaolinmc.archives.ArchiveTree
import com.kaolinmc.archives.transform.TransformerConfig
import com.kaolinmc.archives.transform.TransformerConfig.Companion.plus
import com.kaolinmc.boot.archive.*
import com.kaolinmc.boot.loader.ArchiveClassProvider
import com.kaolinmc.boot.loader.ArchiveSourceProvider
import com.kaolinmc.boot.loader.ClassProvider
import com.kaolinmc.boot.loader.DelegatingClassProvider
import com.kaolinmc.boot.monad.Either
import com.kaolinmc.boot.monad.Tagged
import com.kaolinmc.boot.monad.Tree
import com.kaolinmc.boot.util.mapAsync
import com.kaolinmc.common.util.LazyMap
import com.kaolinmc.common.util.runCatching
import com.kaolinmc.core.app.TargetArtifactRequest
import com.kaolinmc.core.app.TargetDescriptor
import com.kaolinmc.core.app.TargetLinkerResolver
import com.kaolinmc.core.app.TargetRepositorySettings
import com.kaolinmc.core.app.api.ApplicationTarget
import com.kaolinmc.core.entrypoint.Entrypoint
import com.kaolinmc.core.main.ExtensionClassNotFound
import com.kaolinmc.core.main.MainPartitionLoader
import com.kaolinmc.core.minecraft.api.MappingNamespace
import com.kaolinmc.core.minecraft.environment.mappingTargetAttrKey
import com.kaolinmc.core.minecraft.environment.remappersAttrKey
import com.kaolinmc.core.minecraft.remap.ExtensionRemapper
import com.kaolinmc.core.minecraft.remap.MappingContext
import com.kaolinmc.core.minecraft.remap.MappingManager
import com.kaolinmc.core.minecraft.util.parseNode
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment
import com.kaolinmc.tooling.api.exception.StructuredException
import com.kaolinmc.tooling.api.extension.PartitionRuntimeModel
import com.kaolinmc.tooling.api.extension.descriptor
import com.kaolinmc.tooling.api.extension.partition.*
import com.kaolinmc.tooling.api.extension.partition.artifact.PartitionArtifactMetadata
import kotlinx.coroutines.awaitAll
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode

public class MinecraftPartitionMetadata(
    override val name: String,
    public val entrypoint: String?,

    public val archive: ArchiveReference?,

    public val supportedVersions: List<String>,
    public val mappingNamespace: MappingNamespace,
) : ExtensionPartitionMetadata

public open class MinecraftPartitionNode(
    override val archive: ArchiveHandle?,
    override val access: PartitionAccessTree,

    public val entrypoint: Entrypoint?
) : ExtensionPartition

public class MinecraftPartitionLoader(
    private val environment: ExtensionEnvironment,
) : ExtensionPartitionLoader<MinecraftPartitionMetadata> {
    override val id: String = TYPE

    public companion object {
        public const val TYPE: String = "minecraft"
    }

    private var appInheritanceTree: ClassInheritanceTree? = null

    private fun appInheritanceTree(
        app: ApplicationTarget
    ): ClassInheritanceTree {
        fun createPath(
            name: String
        ): ClassInheritancePath? {
            val stream =
                app.node.handle!!.classloader.getResourceAsStream(name.replace('.', '/') + ".class") ?: return null
            val node = ClassNode()
            ClassReader(stream).accept(node, 0)

            return ClassInheritancePath(
                node.name,
                node.superName?.let(::createPath),
                node.interfaces.mapNotNull { n ->
                    createPath(n)
                }
            )
        }

        if (appInheritanceTree == null) {
            appInheritanceTree = LazyMap(HashMap()) {
                createPath(it)
            }
        }

        return appInheritanceTree!!
    }

    override suspend fun cache(
        metadata: PartitionArtifactMetadata,
        parents: List<Tree<Either<PartitionArtifactMetadata, TaggedIArchive>>>,
        helper: PartitionCacheHelper
    ): Tree<Tagged<IArchive<*>, ArchiveNodeResolver<*, *, *, *, *>>> {
        val parents = helper.erm.parents.mapAsync { parent ->
            val main = try {
                helper.cache(
                    MainPartitionLoader.TYPE,
                    parent,
                )
            } catch (_: ArchiveException.ArchiveNotFound) {
                null
            }

            val tweaker = try {
                helper.cache(
                    "tweaker",
                    parent,
                )
            } catch (_: ArchiveException.ArchiveNotFound) {
                null
            }

            listOf(main, tweaker)
        }.awaitAll().flatten().filterNotNull()

        val main = if (helper.erm.namedPartitions.contains("main")) {
            helper.cache("main")
        } else null

        val tweaker = if (helper.erm.namedPartitions.contains("tweaker")) {
            helper.cache("tweaker")
        } else null

        val target = helper.cache(
            TargetArtifactRequest,
            TargetRepositorySettings,
            environment[TargetLinkerResolver]
        )

        return helper.newData(metadata.descriptor, parents + listOf(target) + listOfNotNull(main, tweaker))
    }

    override fun parseMetadata(
        partition: PartitionRuntimeModel,
        reference: ArchiveReference?,
        helper: PartitionMetadataHelper
    ): MinecraftPartitionMetadata {
        val entrypoint = partition.options["entrypoint"]

        val srcNS = partition.options["mappingNS"]
            ?.let(MappingNamespace::parse)
            ?: environment[mappingTargetAttrKey].value

        val supportedVersions = partition.options["versions"]?.split(",")
            ?: throw IllegalArgumentException("Partition: '${partition.name}' in extension: '${helper.erm.descriptor} does not support any versions!")

        return MinecraftPartitionMetadata(
            partition.name,
            entrypoint,
            reference,
            supportedVersions,
            srcNS,
        )
    }

    override fun load(
        metadata: MinecraftPartitionMetadata,
        //  /-- Want to instead use the archive defined in metadata for consistency
        unused: ArchiveReference?,
        // --\
        accessTree: PartitionAccessTree,
        helper: PartitionLoaderHelper
    ): ExtensionPartitionContainer<*, MinecraftPartitionMetadata> {
        val metadata = metadata as MinecraftPartitionMetadata

        val reference = metadata.archive

        return ExtensionPartitionContainer(
            helper.descriptor,
            metadata,
            run {
                val appTarget = environment[ApplicationTarget]

                val manager = environment[MappingManager]

                val mappings = manager[metadata.mappingNamespace]

                val (dependencies, target) = accessTree.targets
                    .map { it.relationship.node }
                    .filterIsInstance<ClassLoadedArchiveNode<*>>()
                    .partition { it.descriptor != TargetDescriptor }

                val handle = reference?.let {
                    remap(
                        reference,
                        mappings,
                        environment[remappersAttrKey]
                            .sortedBy { it.priority },
                        appInheritanceTree(appTarget),
                        accessTree.targets.asSequence()
                            .map(ArchiveTarget::relationship)
                            .map(ArchiveRelationship::node)
                            .filterIsInstance<ClassLoadedArchiveNode<*>>()
                            .mapNotNullTo(ArrayList(), ClassLoadedArchiveNode<*>::handle)
                    )

                    val sourceProviderDelegate = ArchiveSourceProvider(reference)
                    val cl = PartitionClassLoader(
                        helper.descriptor,
                        accessTree,
                        reference,
                        helper.parentClassLoader,
                        sourceProvider = sourceProviderDelegate, classProvider = object : ClassProvider {
                            val dependencyDelegate = DelegatingClassProvider(
                                dependencies
                                    .mapNotNull { it.handle }
                                    .map(::ArchiveClassProvider)
                            )
                            val targetDelegate = target.first().handle!!.classloader

                            override val packages: Set<String> = dependencyDelegate.packages + "*target*"

                            override fun findClass(name: String): Class<*>? {
                                return com.kaolinmc.common.util.runCatching(ClassNotFoundException::class) {
                                    targetDelegate.loadClass(
                                        name
                                    )
                                } ?: dependencyDelegate.findClass(name)
                            }
                        }
                    )

                    PartitionArchiveHandle(
                        "${helper.erm.name}-${metadata.name}",
                        cl,
                        reference,
                        setOf()
                    )
                }

                val instance = handle?.let {
                    metadata.entrypoint?.let { entrypoint ->
                        val extensionClass = runCatching(ClassNotFoundException::class) {
                            handle.classloader.loadClass(
                                entrypoint
                            )
                        } ?: throw StructuredException(
                            ExtensionClassNotFound,
                            description = "Could not init extension because the extension class couldn't be found."
                        ) {
                            entrypoint asContext "Extension class name"
                        }

                        val extensionConstructor =
                            runCatching(NoSuchMethodException::class) { extensionClass.getConstructor() }
                                ?: throw Exception("Could not find no-arg constructor in class: '${entrypoint}' in extension: '${helper.erm.name}'.")

                        extensionConstructor.newInstance() as? Entrypoint
                            ?: throw Exception("Extension class: '$extensionClass' does not extend: '${Entrypoint::class.java.name}'.")
                    }
                }

                MinecraftPartitionNode(
                    handle,
                    accessTree,
                    instance
                )
            }
        )
    }

    private fun remap(
        reference: ArchiveReference,
        context: MappingContext,
        remappers: List<ExtensionRemapper>,
        appTree: ClassInheritanceTree,
        dependencies: List<ArchiveTree>
    ) {
        fun remapTargetToSourcePath(
            path: ClassInheritancePath,
        ): ClassInheritancePath {
            return ClassInheritancePath(
                context.mappings.mapClassName(
                    path.name,
                    context.target,
                    context.source,
                ) ?: path.name,
                path.superClass?.let(::remapTargetToSourcePath),
                path.interfaces.map(::remapTargetToSourcePath)
            )
        }

        fun inheritancePathFor(
            node: ClassNode
        ): ClassInheritancePath {
            fun getParent(name: String?): ClassInheritancePath? {
                if (name == null) return null

                val treeFromApp = appTree[context.mappings.mapClassName(
                    name,
                    context.source,
                    context.target
                ) ?: name]?.let(::remapTargetToSourcePath)

                val treeFromRef = reference.reader["$name.class"]?.let { e ->
                    inheritancePathFor(
                        e.open().parseNode()
                    )
                }

                val treeFromDependencies = dependencies.firstNotNullOfOrNull {
                    it.getResource("$name.class")?.parseNode()?.let(::inheritancePathFor)
                }

                return treeFromApp ?: treeFromRef ?: treeFromDependencies
            }

            return ClassInheritancePath(
                node.name,
                getParent(node.superName),
                node.interfaces.mapNotNull { getParent(it) }
            )
        }

        val treeInternal = (reference.reader.entries())
            .filterNot(ArchiveReference.Entry::isDirectory)
            .filter { it.name.endsWith(".class") }
            .associate { e ->
                val path = inheritancePathFor(e.open().parseNode())
                path.name to path
            }

        val tree = LazyMap { key: String ->
            treeInternal[key] ?: appTree[
                context.mappings.mapClassName(
                    key,
                    context.source,
                    context.target
                ) ?: key
            ]?.let(::remapTargetToSourcePath)
        }

        val config: TransformerConfig = remappers.fold(
            TransformerConfig.of { } as TransformerConfig
        ) { acc: TransformerConfig, it ->
            val config: TransformerConfig = it.remap(
                context,
                tree,
            )

            config + acc
        }

        reference.reader.entries()
            .filter { it.name.endsWith(".class") }
            .forEach { entry ->
//                // TODO, this will recompute frames, see if we need to do that or not.
//                Archives.resolve(
//                    ClassReader(entry.resource.openStream()),
//                    config,
//                )

                reference.writer.put(
                    entry.transform(
                        config, dependencies
                    )
                )
            }
    }
//
//    private fun <A : Annotation, T : MixinInjection.InjectionData> ProcessedMixinContext<A, T>.createMappedTransactionMetadata(
//        destination: String,
//        mappingContext: MappingInjectionProvider.Context
//    ): Job<MixinTransaction.Metadata<T>> = job {
//        val provider = provider as? MappingInjectionProvider<A, T> ?: throw MixinException(
//            message = "Illegal mixin injection provider: '${provider.type}'. Expected it to be a subtype of '${MappingInjectionProvider::class.java.name}' however it was not."
//        ) {
//            solution("Wrap your provider in a mapping provider")
//            solution("Implement '${MappingInjectionProvider::class.java.name}'")
//        }
//
//        MixinTransaction.Metadata(
//            destination,
//            provider.mapData(
//                provider.parseData(this@createMappedTransactionMetadata.context)().merge(),
//                mappingContext
//            )().merge(),
//            provider.get()
//        )
//    }


//    private fun mapperFor(
//        archive: ArchiveReference,
//        dependencies: List<ArchiveTree>,
//        mappings: ArchiveMapping,
//        tree: ClassInheritanceTree,
//        srcNS: String,
//        targetNS: String
//    ): TransformerConfig {
//        fun ClassInheritancePath.fromTreeInternal(): ClassInheritancePath {
//            val mappedName = mappings.mapClassName(name, srcNS, targetNS) ?: name
//
//            return ClassInheritancePath(
//                mappedName,
//                superClass?.fromTreeInternal(),
//                interfaces.map { it.fromTreeInternal() }
//            )
//        }
//
//        val lazilyMappedTree = LazyMap<String, ClassInheritancePath> {
//            tree[mappings.mapClassName(it, srcNS, targetNS)]?.fromTreeInternal()
//        }
//
//        return mappingTransformConfigFor(
//            ArchiveTransformerContext(
//                archive,
//                dependencies,
//                mappings,
//                srcNS,
//                targetNS,
//                lazilyMappedTree,
//            )
//        )
//    }

}