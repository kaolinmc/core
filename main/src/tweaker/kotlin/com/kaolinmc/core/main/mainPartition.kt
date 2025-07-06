package com.kaolinmc.core.main

import com.kaolinmc.archives.ArchiveHandle
import com.kaolinmc.archives.ArchiveReference
import com.kaolinmc.boot.archive.ArchiveException
import com.kaolinmc.boot.archive.ArchiveNodeResolver
import com.kaolinmc.boot.archive.IArchive
import com.kaolinmc.boot.archive.TaggedIArchive
import com.kaolinmc.boot.monad.Either
import com.kaolinmc.boot.monad.Tagged
import com.kaolinmc.boot.monad.Tree
import com.kaolinmc.boot.util.mapAsync
import com.kaolinmc.common.util.runCatching
import com.kaolinmc.core.entrypoint.Entrypoint
import com.kaolinmc.tooling.api.exception.StructuredException
import com.kaolinmc.tooling.api.extension.ExtensionRepository
import com.kaolinmc.tooling.api.extension.PartitionRuntimeModel
import com.kaolinmc.tooling.api.extension.descriptor
import com.kaolinmc.tooling.api.extension.partition.*
import com.kaolinmc.tooling.api.extension.partition.artifact.PartitionArtifactMetadata
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