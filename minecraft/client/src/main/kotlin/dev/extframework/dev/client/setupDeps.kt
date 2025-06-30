@file:JvmName("SetupDeps")

package dev.extframework.dev.client

import dev.extframework.boot.archive.ArchiveGraph
import dev.extframework.boot.dependency.DependencyTypeContainer
import dev.extframework.boot.maven.MavenDependencyResolver
import dev.extframework.boot.maven.MavenResolverProvider
import dev.extframework.`object`.ObjectContainerImpl

internal fun setupDependencyTypes(
    archiveGraph: ArchiveGraph,
): DependencyTypeContainer {
    val maven = MavenDependencyResolver(
        parentClassLoader = ClassLoader.getSystemClassLoader(),
    )

    val dependencyTypes : DependencyTypeContainer = ObjectContainerImpl()
    dependencyTypes.register(MavenResolverProvider(maven))
    archiveGraph.resolvers.register(maven)

    return dependencyTypes
}