package dev.extframework.core.app

import com.durganmcbroom.artifact.resolver.ArtifactMetadata
import com.durganmcbroom.artifact.resolver.ArtifactRepository
import com.durganmcbroom.artifact.resolver.RepositoryFactory
import dev.extframework.archives.ArchiveHandle
import dev.extframework.archives.zip.classLoaderToArchive
import dev.extframework.boot.archive.*
import dev.extframework.boot.monad.Either
import dev.extframework.boot.monad.Tree
import dev.extframework.tooling.api.environment.ExtensionEnvironment.Attribute
import dev.extframework.tooling.api.environment.ExtensionEnvironment.Attribute.Key
import java.nio.file.Path
import java.nio.file.Paths

public class TargetNode(
    override val handle: ArchiveHandle,
) : ClassLoadedArchiveNode<TargetDescriptor> {
    override val descriptor: TargetDescriptor = TargetDescriptor
    override val access: ArchiveAccessTree = object : ArchiveAccessTree {
        override val descriptor: ArtifactMetadata.Descriptor = this@TargetNode.descriptor
        override val targets: List<ArchiveTarget> = listOf()
    }
}

public object TargetArtifactMetadata : ArtifactMetadata<TargetDescriptor, Nothing>(
    TargetDescriptor,
    listOf()
)

public open class TargetLinkerResolver(
    private val linker: TargetLinker,
) : ArchiveNodeResolver<
        TargetDescriptor,
        TargetArtifactRequest,
        TargetNode,
        TargetRepositorySettings,
        TargetArtifactMetadata
        >, Attribute {
    override val metadataType: Class<TargetArtifactMetadata> = TargetArtifactMetadata::class.java
    override val factory: RepositoryFactory<TargetRepositorySettings, ArtifactRepository<TargetRepositorySettings, TargetArtifactRequest, TargetArtifactMetadata>>
        get() = TargetArtifactFactory

    override val id: String = "target"
    override val nodeType: Class<in TargetNode> = TargetNode::class.java

    override val key: Key<*> = TargetLinkerResolver

    public companion object : Key<TargetLinkerResolver>

    override fun deserializeDescriptor(descriptor: Map<String, String>, trace: ArchiveTrace): TargetDescriptor =
        TargetDescriptor

    override fun serializeDescriptor(descriptor: TargetDescriptor): Map<String, String> {
        return mapOf()
    }

    override fun pathForDescriptor(descriptor: TargetDescriptor, classifier: String, type: String): Path {
        return Paths.get("target", "target-$classifier.$type")
    }

    override fun load(
        data: ArchiveData<TargetDescriptor, CachedArchiveResource>,
        accessTree: ArchiveAccessTree,
        helper: ResolutionHelper
    ): TargetNode = TargetNode(
        classLoaderToArchive(linker.targetLoader)
    )

    override suspend fun cache(
        metadata: TargetArtifactMetadata,
        parents: List<Tree<Either<TargetArtifactMetadata, TaggedIArchive>>>,
        helper: CacheHelper<TargetDescriptor>
    ): Tree<TaggedIArchive> {
        return helper.newData(TargetDescriptor, listOf())
    }
}

private object TargetArtifactFactory :
    RepositoryFactory<TargetRepositorySettings, ArtifactRepository<TargetRepositorySettings, TargetArtifactRequest, TargetArtifactMetadata>> {
    override fun createNew(settings: TargetRepositorySettings): ArtifactRepository<TargetRepositorySettings, TargetArtifactRequest, TargetArtifactMetadata> {
        return TargetArtifactRepository
    }
}

private object TargetArtifactRepository :
    ArtifactRepository<TargetRepositorySettings, TargetArtifactRequest, TargetArtifactMetadata> {
    override val factory: RepositoryFactory<TargetRepositorySettings, ArtifactRepository<TargetRepositorySettings, TargetArtifactRequest, TargetArtifactMetadata>> =
        TargetArtifactFactory

    override val name: String = "target"
    override val settings: TargetRepositorySettings = TargetRepositorySettings

    override suspend fun get(request: TargetArtifactRequest): TargetArtifactMetadata =
        TargetArtifactMetadata
}
