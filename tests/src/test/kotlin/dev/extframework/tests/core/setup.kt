package dev.extframework.tests.core

import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenDescriptor
import dev.extframework.boot.archive.ArchiveGraph
import dev.extframework.boot.archive.ArchiveTreeAuditContext
import dev.extframework.boot.archive.ArchiveTreeAuditor
import dev.extframework.boot.archive.DefaultArchiveGraph
import dev.extframework.boot.dependency.DependencyResolverProvider
import dev.extframework.boot.dependency.DependencyTypeContainer
import dev.extframework.boot.maven.MavenConstraintNegotiator
import dev.extframework.boot.maven.MavenDependencyResolver
import dev.extframework.boot.maven.MavenResolverProvider
import dev.extframework.boot.monad.removeIf
import dev.extframework.common.util.readInputStream
import dev.extframework.extloader.DefaultExtensionEnvironment
import dev.extframework.extloader.DefaultExtensionLoader
import dev.extframework.extloader.environment.registerLoaders
import dev.extframework.extloader.extension.DefaultExtensionResolver
import dev.extframework.`object`.ObjectContainerImpl
import dev.extframework.tooling.api.ExtensionLoader
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import dev.extframework.tooling.api.environment.ObjectContainerAttribute
import dev.extframework.tooling.api.environment.ValueAttribute
import dev.extframework.tooling.api.environment.dependencyTypesAttrKey
import dev.extframework.tooling.api.environment.partitionLoadersAttrKey
import dev.extframework.tooling.api.environment.wrkDirAttrKey
import java.nio.file.Path
import kotlin.io.path.Path

private class THIS


public fun newLoader(path: Path = Path("tests/cache")): Pair<ExtensionLoader, ExtensionEnvironment> {
    val (graph, types) = setupBoot(path)
    val environment = DefaultExtensionEnvironment(
        "root",
    )

    environment += ValueAttribute(wrkDirAttrKey, path)
    environment += ObjectContainerAttribute(dependencyTypesAttrKey, types)
    environment += ObjectContainerAttribute(partitionLoadersAttrKey)
    environment[partitionLoadersAttrKey].registerLoaders()

    val loader = DefaultExtensionLoader(
        DefaultExtensionResolver(
            ClassLoader.getSystemClassLoader(),
            environment
        ),
        graph,
        environment,
    )

    environment += loader

    return Pair(loader, environment)
}

public fun setupBoot(path: Path): Pair<ArchiveGraph, DependencyTypeContainer> {
    val dependencies = THIS::class.java.getResource("/dependencies.txt")?.openStream()?.use {
        val fileStr = String(it.readInputStream())
        fileStr.split("\n").toSet()
    }?.filterNot { it.isBlank() }?.mapTo(HashSet()) { SimpleMavenDescriptor.parseDescription(it)!! }
        ?: throw IllegalStateException("Cant load dependencies?")

    val archiveGraph = DefaultArchiveGraph(
        path,
    )

    val negotiator = MavenConstraintNegotiator()

    val alreadyLoaded = dependencies.map {
        negotiator.classify(it)
    }

    archiveGraph.auditors = archiveGraph.auditors.chain(object : ArchiveTreeAuditor {
        override fun audit(event: ArchiveTreeAuditContext): ArchiveTreeAuditContext {
            return event.copy(tree = event.tree.removeIf {
                alreadyLoaded.contains(
                    negotiator.classify(
                        it.value.descriptor as? SimpleMavenDescriptor ?: return@removeIf false
                    )
                )
            }!!)
        }
    })

    val maven = MavenDependencyResolver(
        parentClassLoader = THIS::class.java.classLoader,
    )

    archiveGraph.resolvers.register(maven)

    return archiveGraph to ObjectContainerImpl<DependencyResolverProvider<*, *, *>>().apply {
        register (MavenResolverProvider(resolver = maven))
    }
}