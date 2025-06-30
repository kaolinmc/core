package dev.extframework.core.main

import dev.extframework.archives.ArchiveHandle
import dev.extframework.archives.ArchiveReference
import dev.extframework.boot.archive.ArchiveException
import dev.extframework.boot.archive.ArchiveNodeResolver
import dev.extframework.boot.archive.IArchive
import dev.extframework.boot.archive.TaggedIArchive
import dev.extframework.boot.monad.Either
import dev.extframework.boot.monad.Tagged
import dev.extframework.boot.monad.Tree
import dev.extframework.boot.util.mapAsync
import dev.extframework.common.util.runCatching
import dev.extframework.core.entrypoint.Entrypoint
import dev.extframework.tooling.api.exception.StructuredException
import dev.extframework.tooling.api.extension.ExtensionRepository
import dev.extframework.tooling.api.extension.PartitionRuntimeModel
import dev.extframework.tooling.api.extension.descriptor
import dev.extframework.tooling.api.extension.partition.*
import dev.extframework.tooling.api.extension.partition.artifact.PartitionArtifactMetadata
import kotlinx.coroutines.awaitAll

public class MainPartitionLoader : ExtensionPartitionLoader<MainPartitionMetadata> {
    override val id: String = TYPE

    public companion object {
        public const val TYPE: String = "main"
    }

    override fun parseMetadata(
        partition: PartitionRuntimeModel, reference: ArchiveReference?, helper: PartitionMetadataHelper
    ): MainPartitionMetadata = MainPartitionMetadata(
        partition.options["extension-class"]
            ?: throw IllegalArgumentException("Main partition from extension: '${helper.erm.descriptor}' must contain an extension class defined as option: 'extension-class'."),
        reference,
        partition.repositories,
        partition.dependencies
    )

    override suspend fun cache(
        metadata: PartitionArtifactMetadata,
        parents: List<Tree<Either<PartitionArtifactMetadata, TaggedIArchive>>>,
        helper: PartitionCacheHelper
    ): Tree<Tagged<IArchive<*>, ArchiveNodeResolver<*, *, *, *, *>>> {
        val parentMainPartitions = helper.erm.parents.mapAsync {
            try {
                helper.cache("main", it)
            } catch (_: ArchiveException.ArchiveNotFound) {
                null
            }
        }

        val parentTweakerPartitions = helper.erm.parents.mapAsync {
            try {
                helper.cache("tweaker", it)
            } catch (_: ArchiveException.ArchiveNotFound) {
                null
            }
        }

        val tweakerPartition = if (helper.erm.partitions.any { model -> model.name == "tweaker" }) {
            listOf(helper.cache("tweaker"))
        } else listOf()

        return helper.newData(
            metadata.descriptor,
            parentMainPartitions.awaitAll().filterNotNull() + parentTweakerPartitions.awaitAll()
                .filterNotNull() + tweakerPartition
        )
    }

    override fun load(
        metadata: MainPartitionMetadata,
        reference: ArchiveReference?,
        accessTree: PartitionAccessTree,
        helper: PartitionLoaderHelper
    ): ExtensionPartitionContainer<*, MainPartitionMetadata> {
        val thisDescriptor = helper.descriptor

        return ExtensionPartitionContainer(
            thisDescriptor,
            metadata,
            run { // The reason we use a target enabled partition for this one is because main depends on the feature partition which is target enabled. It doesnt make sense for a non target enabled partition to rely on an enabled one.
                val cl = reference?.let {
                    PartitionClassLoader(
                        thisDescriptor,
                        accessTree,
                        it,
                        helper.parentClassLoader,
                    )
                }

                val handle = cl?.let {
                    PartitionArchiveHandle(
                        "${helper.erm.name}-main", cl, reference, setOf()
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
                        description = "Could not init extension because the extension class couldn't be found."
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
                    handle, accessTree, instance
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
    override val archive: ArchiveHandle?, override val access: PartitionAccessTree, public val entrypoint: Entrypoint?
) : ExtensionPartition