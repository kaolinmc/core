package com.kaolinmc.dev.client

import com.durganmcbroom.artifact.resolver.ArtifactMetadata
import com.kaolinmc.archives.ArchiveHandle
import com.kaolinmc.archives.zip.ZipFinder
import com.kaolinmc.archives.zip.classLoaderToArchive
import com.kaolinmc.boot.archive.ArchiveAccessTree
import com.kaolinmc.boot.archive.ArchiveTarget
import com.kaolinmc.boot.archive.ClassLoadedArchiveNode
import com.kaolinmc.boot.loader.*
import com.kaolinmc.core.app.api.ApplicationDescriptor
import com.kaolinmc.core.minecraft.api.MinecraftAppApi
import java.nio.file.Path

private fun emptyAccess(version: String) = object : ArchiveAccessTree {
    override val descriptor: ArtifactMetadata.Descriptor = ApplicationDescriptor(
        "net.minecraft",
        "client",
        version,
        null
    )
    override val targets: List<ArchiveTarget> = listOf()
}

internal class ClasspathApp(
    override val classpath: List<Path>,
    override val version: String,
    override val path: Path,
    override val gameJar: Path,
    override val mainClass: String
) : MinecraftAppApi() {
    override val gameDir: Path = path

    override val node: ClassLoadedArchiveNode<ApplicationDescriptor> =
        object : ClassLoadedArchiveNode<ApplicationDescriptor> {
            override val descriptor: ApplicationDescriptor = ApplicationDescriptor(
                "net.minecraft",
                "client",
                version,
                null
            )

            override val access: ArchiveAccessTree = object : ArchiveAccessTree {
                override val descriptor: ArtifactMetadata.Descriptor = ApplicationDescriptor(
                    "net.minecraft",
                    "client",
                    version,
                    null
                )
                override val targets: List<ArchiveTarget> = listOf()
            }
            private val references = classpath.map { it ->
                ZipFinder.find(it)
            }

            override val handle: ArchiveHandle = classLoaderToArchive(
                MutableClassLoader(
                    name = "minecraft-loader",
                    resources = MutableResourceProvider(
                        references.mapTo(ArrayList()) { ArchiveResourceProvider(it) }
                    ),
                    sources = MutableSourceProvider(
                        references.mapTo(ArrayList()) { ArchiveSourceProvider(it) }
                    ),
                    parent = ClassLoader.getSystemClassLoader(),
                )
            )
        }
}