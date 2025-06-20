package dev.extframework.dev.client

import com.durganmcbroom.artifact.resolver.*
import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenDescriptor
import dev.extframework.boot.archive.*
import dev.extframework.boot.dependency.BasicDependencyNode
import dev.extframework.boot.maven.MavenConstraintNegotiator
import dev.extframework.boot.monad.Either
import dev.extframework.boot.monad.Tagged
import dev.extframework.boot.monad.Tree
import dev.extframework.boot.monad.removeIf
import dev.extframework.boot.util.typeOf
import dev.extframework.common.util.readInputStream
import java.nio.file.Path
import kotlin.io.path.Path


internal fun addPackagedDependencies(
    archiveGraph: ArchiveGraph,
    packagedDependencies: Set<SimpleMavenDescriptor>
) {
    val negotiator = MavenConstraintNegotiator()

    val alreadyLoaded = packagedDependencies.mapTo(HashSet()) {
        negotiator.classify(it)
    }

    val packagedDependencyRemover = object : ArchiveTreeAuditor {
        override fun audit(event: ArchiveTreeAuditContext): ArchiveTreeAuditContext {
           return event.copy(tree = event.tree.removeIf {
                alreadyLoaded.contains(negotiator.classify(it.value.descriptor as? SimpleMavenDescriptor ?: return@removeIf false))
            }!!)
        }
    }

    archiveGraph.auditors = archiveGraph.auditors.chain(packagedDependencyRemover)
}

private class THIS

internal fun parsePackagedDependencies(): Set<SimpleMavenDescriptor> {
    val dependencies: java.util.HashSet<SimpleMavenDescriptor> =
        THIS::class.java.getResourceAsStream("/dependencies.txt")?.use {
            val fileStr = String(it.readInputStream())
            fileStr.split("\n").toSet()
        }?.filterNot { it.isBlank() }?.mapTo(HashSet()) { SimpleMavenDescriptor.parseDescription(it)!! }
            ?: throw IllegalStateException("Cant load dependencies?")

    return dependencies
}

internal fun setupArchiveGraph(
    path: Path,
): ArchiveGraph {
//    val primordial = PrimordialNodeResolver()

    val archiveGraph = DefaultArchiveGraph(
        path,
    )

//    archiveGraph.registerResolver(primordial)

    return archiveGraph
}

//internal class PrimordialNodeResolver :
//    ArchiveNodeResolver<ArtifactMetadata.Descriptor, ArtifactRequest<ArtifactMetadata.Descriptor>, BasicDependencyNode<ArtifactMetadata.Descriptor>, RepositorySettings, ArtifactMetadata<ArtifactMetadata.Descriptor, *>> {
//    override val metadataType: Class<ArtifactMetadata<ArtifactMetadata.Descriptor, *>> = typeOf()
//    override val factory: RepositoryFactory<RepositorySettings, ArtifactRepository<RepositorySettings, ArtifactRequest<ArtifactMetadata.Descriptor>, ArtifactMetadata<ArtifactMetadata.Descriptor, *>>>
//        get() =  throw UnsupportedOperationException()
//    override val name: String = "primordial"
//    override val nodeType: Class<in BasicDependencyNode<ArtifactMetadata.Descriptor>> = typeOf()
//
//    override fun deserializeDescriptor(
//        descriptor: Map<String, String>,
//        trace: ArchiveTrace
//    ): ArtifactMetadata.Descriptor {
//        throw ArchiveException(trace, "Operation not supported")
//    }
//
//    override fun serializeDescriptor(descriptor: ArtifactMetadata.Descriptor): Map<String, String> {
//        return mapOf()
//    }
//
//    override fun pathForDescriptor(
//        descriptor: ArtifactMetadata.Descriptor,
//        classifier: String,
//        type: String
//    ): Path {
//        return Path("")
//    }
//
//    override fun load(
//        data: ArchiveData<ArtifactMetadata.Descriptor, CachedArchiveResource>,
//        accessTree: ArchiveAccessTree,
//        helper: ResolutionHelper
//    ): BasicDependencyNode<ArtifactMetadata.Descriptor> {
//        throw ArchiveException(helper.trace, "Operation not supported")
//    }
//
//    override suspend fun cache(
//        metadata: ArtifactMetadata<ArtifactMetadata.Descriptor, *>,
//        parents: List<Tree<Either<ArtifactMetadata<ArtifactMetadata.Descriptor, *>, TaggedIArchive>>>,
//        helper: CacheHelper<ArtifactMetadata.Descriptor>
//    ): Tree<TaggedIArchive> {
//        TODO("Not yet implemented")
//    }
//
//    override suspend fun cache(
//        artifact: Artifact<ArtifactMetadata<ArtifactMetadata.Descriptor, *>>,
//        helper: CacheHelper<ArtifactMetadata.Descriptor>
//    ): Tree<Tagged<IArchive<*>, ArchiveNodeResolver<*, *, *, *, *>>> {
//        throw ArchiveException(helper.trace, "Operation not supported")
//    }
//}

