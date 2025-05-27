package dev.extframework.core.main

import com.durganmcbroom.artifact.resolver.Artifact
import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.async.AsyncJob
import com.durganmcbroom.jobs.async.asyncJob
import com.durganmcbroom.jobs.async.mapAsync
import com.durganmcbroom.jobs.job
import dev.extframework.archives.ArchiveHandle
import dev.extframework.archives.ArchiveReference
import dev.extframework.boot.archive.ArchiveException
import dev.extframework.boot.archive.ArchiveNodeResolver
import dev.extframework.boot.archive.IArchive
import dev.extframework.boot.monad.Tagged
import dev.extframework.boot.monad.Tree
import dev.extframework.common.util.runCatching
import dev.extframework.core.entrypoint.Entrypoint
import dev.extframework.tooling.api.exception.StructuredException
import dev.extframework.tooling.api.extension.ExtensionRepository
import dev.extframework.tooling.api.extension.PartitionRuntimeModel
import dev.extframework.tooling.api.extension.descriptor
import dev.extframework.tooling.api.extension.partition.*
import dev.extframework.tooling.api.extension.partition.artifact.PartitionArtifactMetadata
import dev.extframework.tooling.api.extension.partition.artifact.partition
import kotlinx.coroutines.awaitAll

public class MainPartitionLoader(
//    private val environment: ExtensionEnvironment
) : ExtensionPartitionLoader<MainPartitionMetadata> {
    override val type: String = TYPE

    public companion object {
        public const val TYPE: String = "main"
    }

    override fun parseMetadata(
        partition: PartitionRuntimeModel,
        reference: ArchiveReference?,
        helper: PartitionMetadataHelper
    ): Job<MainPartitionMetadata> = job {
//        val processor = environment[AnnotationProcessor].extract()

//        val allFeatures = reference?.let {
//            it.reader.entries()
//                .filter { it.name.endsWith(".class") }
//                .map {
//                    it.open().parseNode()
//                }.filter {
//                    it.definesFeatures(processor)
//                }.flatMap {
//                    it.findDefinedFeatures(processor)
//                }.toList()
//        } ?: listOf()

        MainPartitionMetadata(
//            allFeatures,
            partition.options["extension-class"]
                ?: throw IllegalArgumentException("Main partition from extension: '${helper.erm.descriptor}' must contain an extension class defined as option: 'extension-class'."),
            reference,
            partition.repositories,
            partition.dependencies
        )
    }

    override fun cache(
        artifact: Artifact<PartitionArtifactMetadata>,
        helper: PartitionCacheHelper
    ): AsyncJob<Tree<Tagged<IArchive<*>, ArchiveNodeResolver<*, *, *, *, *>>>> = asyncJob {
//        val featurePartitionName = "feature-holder-${UUID.randomUUID()}"
//
//        val featurePartition = PartitionRuntimeModel(
//            FeaturePartitionLoader.TYPE,
//            featurePartitionName,
//
//            helper.prm.repositories,
//            helper.prm.dependencies,
//            mapOf()
//        )
//
//        val newPartition = helper.newPartition(
//            featurePartition
//        )().merge()

//        val coreTree = helper.cache(
//            PartitionArtifactRequest(THIS_DESCRIPTOR),
//            ExtensionRepositorySettings.local(), // Doesn't matter, won't ever get checked,
//            environment[ExtensionResolver].extract().partitionResolver
//        )().merge()

        val parentMainPartitions = helper.erm.parents.mapAsync {
            val result = helper.cache("main", helper.defaultEnvironment, it)()

            val ex = result.exceptionOrNull()
            if (ex != null) {
                if (ex is ArchiveException.ArchiveNotFound) null
                else throw ex
            }

            result.getOrNull()
        }

        val parentTweakerPartitions = helper.erm.parents.mapAsync {
            val result = helper.cache("tweaker", helper.defaultEnvironment, it)()

            val ex = result.exceptionOrNull()
            if (ex != null) {
                if (ex is ArchiveException.ArchiveNotFound) null
                else throw ex
            }

            result.getOrNull()
        }

        val tweakerPartition = if (helper.erm.partitions.any { model -> model.name == "tweaker" }) {
            listOf(helper.cache("tweaker", helper.defaultEnvironment)().merge())
        } else listOf()

        helper.newData(
            artifact.metadata.descriptor,
            parentMainPartitions.awaitAll().filterNotNull()
                    + parentTweakerPartitions.awaitAll().filterNotNull()
                    + tweakerPartition
        )
    }

    override fun load(
        metadata: MainPartitionMetadata,
        reference: ArchiveReference?,
        accessTree: PartitionAccessTree,
        helper: PartitionLoaderHelper
    ): Job<ExtensionPartitionContainer<*, MainPartitionMetadata>> = job {
        val thisDescriptor = helper.descriptor

        ExtensionPartitionContainer(
            thisDescriptor,
            metadata,
            run { // The reason we use a target enabled partition for this one is because main depends on the feature partition which is target enabled. It doesnt make sense for a non target enabled partition to rely on an enabled one.
                val cl = reference?.let {
//                    writeFeaturesInto(
//                        helper,
//                        it
//                    )().merge()

//                    val sourceProviderDelegate = ArchiveSourceProvider(it)

                    PartitionClassLoader(
                        thisDescriptor,
                        accessTree,
                        it,
                        helper.parentClassLoader,
//                        sourceProvider = object : SourceProvider by sourceProviderDelegate {
//                            private val featureContainers =
//                                metadata.definedFeatures.mapTo(HashSet()) { it.container.withDots() }
//
//                            override fun findSource(name: String): ByteBuffer? {
//                                return if (featureContainers.contains(name)) null
//                                else sourceProviderDelegate.findSource(name)
//                            }
//                        }
                    )
                }

                val handle = cl?.let {
                    PartitionArchiveHandle(
                        "${helper.erm.name}-main",
                        cl,
                        reference,
                        setOf()
                    )
                }

                val extensionClassName = metadata.extensionClass

                val instance = handle?.let {
                    val extensionClass = runCatching(ClassNotFoundException::class) {
                        handle.classloader.loadClass(
                            extensionClassName
                        )
                    } ?: throw StructuredException(
                        ExtensionClassNotFound,
                        message = "Could not init extension because the extension class couldn't be found."
                    ) {
                        extensionClassName asContext "Extension class name"
                    }

                    val extensionConstructor =
                        runCatching(NoSuchMethodException::class) { extensionClass.getConstructor() }
                            ?: throw Exception("Could not find no-arg constructor in class: '${extensionClassName}' in extension: '${helper.erm.name}'.")

                    extensionConstructor.newInstance() as? Entrypoint
                        ?: throw Exception("Extension class: '$extensionClass' does not extend: '${Entrypoint::class.java.name}'.")
                }

                MainPartitionNode(
                    handle,
                    accessTree,
                    instance
                )
            })
    }
}

public data class MainPartitionMetadata(
//    val definedFeatures: List<FeatureReference>,
    val extensionClass: String,
    val archive: ArchiveReference?,

    internal val repositories: List<ExtensionRepository>,
    internal val dependencies: Set<Map<String, String>>,
) : ExtensionPartitionMetadata {
    override val name: String = "main"
}

public data class MainPartitionNode(
    override val archive: ArchiveHandle?,
    override val access: PartitionAccessTree,
    public val entrypoint: Entrypoint?
) : ExtensionPartition