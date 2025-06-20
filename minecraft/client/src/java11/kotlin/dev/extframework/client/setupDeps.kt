@file:JvmName("SetupDeps")

package dev.extframework.client

import dev.extframework.boot.archive.ArchiveGraph
import dev.extframework.boot.archive.JpmResolutionProvider
import dev.extframework.boot.dependency.DependencyTypeContainer
import dev.extframework.boot.maven.MavenDependencyResolver
import dev.extframework.boot.maven.MavenResolverProvider

internal fun setupDependencyTypes(
    archiveGraph: ArchiveGraph,
): DependencyTypeContainer {
    val maven = MavenDependencyResolver(
        parentClassLoader = ClassLoader.getSystemClassLoader(),
        resolutionProvider = JpmResolutionProvider
    )

    val dependencyTypes = DependencyTypeContainer(archiveGraph)
    dependencyTypes.register("simple-maven", MavenResolverProvider(maven))

    return dependencyTypes
}