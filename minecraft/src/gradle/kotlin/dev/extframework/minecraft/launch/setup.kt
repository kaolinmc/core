package dev.extframework.minecraft.launch

import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenDescriptor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import dev.extframework.archive.mapper.ArchiveMapping
import dev.extframework.archive.mapper.transform.transformArchive
import dev.extframework.archives.Archives
import dev.extframework.archives.zip.ZipFinder
import dev.extframework.boot.util.mapAsync
import dev.extframework.common.util.copyTo
import dev.extframework.common.util.make
import dev.extframework.common.util.resolve
import dev.extframework.core.minecraft.api.MappingNamespace
import dev.extframework.core.minecraft.util.write
import dev.extframework.launchermeta.handler.*
import dev.extframework.minecraft.MojangNamespaces
import kotlinx.coroutines.awaitAll
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.Logger
import kotlin.io.path.Path
import kotlin.io.path.exists


public data class MinecraftStartMetadata(
    val version: String,
    val clientJar: Path,
    val nativesDir: Path,
    val libraries: List<Path>,
    val assets: Path,
    val assetIndex: String,
    val arguments: Arguments,
    val mainClass: String,
)

public fun remapMinecraft(
    metadata: MinecraftStartMetadata,
    repository: Path,
    mappings: ArchiveMapping,
    targetNamespace: MappingNamespace
) : Path {
    val minecraftPath = repository resolve "net" resolve "minecraft" resolve "client" resolve metadata.version resolve targetNamespace.path

    val minecraftJarPath = minecraftPath resolve "minecraft-${metadata.version}.jar"

    if (!minecraftJarPath.exists()) {
        val archive = Archives.find(metadata.clientJar, ZipFinder)
        val libArchives = metadata.libraries.map(ZipFinder::find)

        transformArchive(
            archive,
            libArchives,
            mappings,
            MojangNamespaces.obfuscated.identifier,
            targetNamespace.identifier,
        )

        archive.write(minecraftJarPath)
    }

    return minecraftJarPath
}

public suspend fun setupMinecraft(
    version: String,
    path: Path,
    logger: Logger
): MinecraftStartMetadata {
    val mapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val versionsPath = path resolve "versions" resolve version
    val manifestPath = versionsPath resolve "$version.json"
    if (manifestPath.make()) {
        val manifest = loadVersionManifest().find(version)
            ?: throw IllegalStateException("Failed to find minecraft version: '${version}'. Looked in: 'https://launchermeta.mojang.com/mc/game/version_manifest_v2.json'.")

        manifest.metadata().getOrThrow() copyTo manifestPath
    }

    val metadata = mapper.readValue<LaunchMetadata>(manifestPath.toFile())

    val clientPath = versionsPath resolve "$version.jar"
    if (clientPath.make()) {
        val mcJar = metadata.downloads[LaunchMetadataDownloadType.CLIENT]
            ?.toResource()?.getOrThrow()
            ?: throw IllegalArgumentException("Cant find client in launch metadata?")

        mcJar copyTo clientPath
    }

    val metadataProcessor = DefaultMetadataProcessor()

    val libraryPath = path resolve "libraries"

    val extractPath = path resolve "bin"

    val librariesPath = metadata.libraries.mapAsync { lib ->
        val descriptor = SimpleMavenDescriptor.parseDescription(lib.name)!!

        val rawArtifacts = metadataProcessor.deriveArtifacts(OsType.type, lib)
        val artifacts =
            rawArtifacts
                .mapAsync { artifact ->
                    val jarPath = libraryPath resolve (artifact.path
                        ?: "temp${File.separator}${descriptor.name}-${descriptor.version}.jar")

                    if (jarPath.make())
                        artifact.toResource().getOrThrow() copyTo jarPath

                    jarPath
                }.awaitAll()

        val extract = lib.extract
        if (extract != null) {
            artifacts.forEach { t ->
                ZipFinder.find(t).use { archive ->
                    archive.reader.entries()
                        .filterNot { entry ->
                            extract.exclude.any { exclude ->
                                entry.name.startsWith(exclude)
                            }
                        }.forEach { entry ->
                            val path = extractPath resolve entry.name

                            if (path.make()) {
                                entry.open().copyTo(path.toFile().outputStream())
                            }
                        }
                }
            }
        }

        rawArtifacts.mapNotNull { it.path }.map {
            libraryPath resolve it
        }
    }.awaitAll().flatten()

    downloadAssets(
        metadata = metadata,
        path resolve "assets" resolve "objects",
        path resolve "assets" resolve "indexes" resolve "${metadata.assetIndex.id}.json",
        logger
    )

   return MinecraftStartMetadata(
        version,
        clientPath,
        extractPath,
        librariesPath,
        path resolve "assets",
        metadata.assetIndex.id,
        metadata.arguments ?: metadata.minecraftArguments?.let {
            Arguments(
                it.split(" ").map { s -> Argument.Value(ValueType.StringValue(s)) },
                listOf(Argument.Value(ValueType.StringValue("-Djava.library.path=\${natives_directory}")))
            )
        } ?: Arguments(
            listOf(),
            listOf(
                Argument.Value(ValueType.StringValue("-Djava.library.path=\${natives_directory}"))
            )
        ),
        metadata.mainClass,
    )
}

internal fun getMinecraftDir(): Path {
    val osName = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")

    return when {
        osName.contains("win") -> {
            val appData = System.getenv("APPDATA")?.let(::Path) ?: Path(userHome, "AppData", "Roaming")
            appData resolve ".minecraft"
        }

        osName.contains("mac") -> Paths.get(userHome, "Library", "Application Support", "minecraft")
        else -> Paths.get(userHome, ".minecraft") // Assuming Linux/Unix-like
    }
}