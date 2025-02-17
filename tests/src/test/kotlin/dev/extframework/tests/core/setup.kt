package dev.extframework.tests.core

import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenDescriptor
import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import dev.extframework.boot.archive.ArchiveGraph
import dev.extframework.boot.archive.ArchiveTreeAuditContext
import dev.extframework.boot.archive.ArchiveTreeAuditor
import dev.extframework.boot.archive.DefaultArchiveGraph
import dev.extframework.boot.dependency.DependencyTypeContainer
import dev.extframework.boot.maven.MavenConstraintNegotiator
import dev.extframework.boot.maven.MavenDependencyResolver
import dev.extframework.boot.maven.MavenResolverProvider
import dev.extframework.boot.monad.removeIf
import dev.extframework.common.util.readInputStream
import java.nio.file.Path

private class THIS

fun setupBoot(path: Path): Pair<ArchiveGraph, DependencyTypeContainer> {
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
        override fun audit(event: ArchiveTreeAuditContext): Job<ArchiveTreeAuditContext> = job {
            event.copy(tree = event.tree.removeIf {
                alreadyLoaded.contains(negotiator.classify(it.value.descriptor as? SimpleMavenDescriptor ?: return@removeIf false))
            }!!)
        }
    })

    val maven = MavenDependencyResolver(
        parentClassLoader = THIS::class.java.classLoader,
    )

    archiveGraph.registerResolver(maven)

    return archiveGraph to DependencyTypeContainer(archiveGraph).apply {
        register("simple-maven", MavenResolverProvider(resolver = maven))
    }
}