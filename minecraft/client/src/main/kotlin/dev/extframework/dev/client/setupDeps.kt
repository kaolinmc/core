@file:JvmName("SetupDeps")

package dev.extframework.dev.client

import dev.extframework.boot.archive.ArchiveGraph
import dev.extframework.boot.dependency.DependencyTypeContainer
import dev.extframework.boot.maven.MavenDependencyResolver
import dev.extframework.boot.maven.MavenResolverProvider

internal fun setupDependencyTypes(
    archiveGraph: ArchiveGraph,
): DependencyTypeContainer {
    val maven = MavenDependencyResolver(
        parentClassLoader = ClassLoader.getSystemClassLoader(),
    )

    val dependencyTypes = DependencyTypeContainer(archiveGraph)
    dependencyTypes.register("simple-maven", MavenResolverProvider(maven))

    return dependencyTypes
}