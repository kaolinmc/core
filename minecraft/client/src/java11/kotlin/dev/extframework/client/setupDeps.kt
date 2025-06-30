@file:JvmName("SetupDeps")

package dev.extframework.client

import dev.extframework.boot.archive.ArchiveGraph
import dev.extframework.boot.archive.JpmResolutionProvider
import dev.extframework.boot.dependency.DependencyTypeContainer
import dev.extframework.boot.maven.MavenDependencyResolver
import dev.extframework.boot.maven.MavenResolverProvider
import dev.extframework.`object`.ObjectContainerImpl

internal fun setupDependencyTypes(
    archiveGraph: ArchiveGraph,
): DependencyTypeContainer {
    val maven = MavenDependencyResolver(
        parentClassLoader = ClassLoader.getSystemClassLoader(),
        resolutionProvider = JpmResolutionProvider
    )

    val dependencyTypes : DependencyTypeContainer = ObjectContainerImpl()
    dependencyTypes.register(MavenResolverProvider(maven))
    archiveGraph.resolvers.register(maven)

    return dependencyTypes
}