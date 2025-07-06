package com.kaolinmc.tests.core

import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenDescriptor
import com.kaolinmc.boot.archive.ArchiveGraph
import com.kaolinmc.boot.archive.ArchiveTreeAuditContext
import com.kaolinmc.boot.archive.ArchiveTreeAuditor
import com.kaolinmc.boot.archive.DefaultArchiveGraph
import com.kaolinmc.boot.dependency.DependencyResolverProvider
import com.kaolinmc.boot.dependency.DependencyTypeContainer
import com.kaolinmc.boot.maven.MavenConstraintNegotiator
import com.kaolinmc.boot.maven.MavenDependencyResolver
import com.kaolinmc.boot.maven.MavenResolverProvider
import com.kaolinmc.boot.monad.removeIf
import com.kaolinmc.common.util.readInputStream
import com.kaolinmc.extloader.DefaultExtensionEnvironment
import com.kaolinmc.extloader.DefaultExtensionLoader
import com.kaolinmc.extloader.environment.registerLoaders
import com.kaolinmc.extloader.extension.DefaultExtensionResolver
import com.kaolinmc.`object`.ObjectContainerImpl
import com.kaolinmc.tooling.api.ExtensionLoader
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment
import com.kaolinmc.tooling.api.environment.ObjectContainerAttribute
import com.kaolinmc.tooling.api.environment.ValueAttribute
import com.kaolinmc.tooling.api.environment.dependencyTypesAttrKey
import com.kaolinmc.tooling.api.environment.partitionLoadersAttrKey
import com.kaolinmc.tooling.api.environment.wrkDirAttrKey
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