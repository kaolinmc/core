@file:JvmName("SetupDepsa")

package com.kaolinmc.client

import com.kaolinmc.boot.archive.ArchiveGraph
import com.kaolinmc.boot.archive.JpmResolutionProvider
import com.kaolinmc.boot.dependency.DependencyTypeContainer
import com.kaolinmc.boot.maven.MavenDependencyResolver
import com.kaolinmc.boot.maven.MavenResolverProvider
import com.kaolinmc.`object`.ObjectContainerImpl

//internal fun setupDependencyTypes(
//    archiveGraph: ArchiveGraph,
//): DependencyTypeContainer {
//    val maven = MavenDependencyResolver(
//        parentClassLoader = ClassLoader.getSystemClassLoader(),
//        resolutionProvider = JpmResolutionProvider
//    )
//
//    val dependencyTypes : DependencyTypeContainer = ObjectContainerImpl()
//    dependencyTypes.register(MavenResolverProvider(maven))
//    archiveGraph.resolvers.register(maven)
//
//    return dependencyTypes
//}