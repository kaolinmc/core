package dev.extframework.minecraft.launch

import com.durganmcbroom.resources.RemoteResource
import com.fasterxml.jackson.module.kotlin.readValue
import dev.extframework.boot.util.basicObjectMapper
import dev.extframework.boot.util.mapAsync
import dev.extframework.common.util.copyTo
import dev.extframework.common.util.make
import dev.extframework.common.util.resolve
import dev.extframework.launchermeta.handler.AssetIndex
import dev.extframework.launchermeta.handler.LaunchMetadata
import dev.extframework.launchermeta.handler.assetIndex
import io.ktor.client.request.*
import kotlinx.coroutines.delay
import java.net.URL
import java.nio.file.Path
import java.util.logging.Logger
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

internal const val MINECRAFT_RESOURCES_URL: String = "https://resources.download.minecraft.net"

internal suspend fun downloadAssets(
    metadata: LaunchMetadata,
    assetsObjectsPath: Path,
    assetIndexCachePath: Path,
    logger: Logger
) {
    if (assetIndexCachePath.make()) {
        val assetIndexCacheResource = metadata.assetIndex().getOrThrow()

        assetIndexCacheResource copyTo assetIndexCachePath
    }

    val objects = basicObjectMapper.readValue<AssetIndex>(assetIndexCachePath.toFile()).objects
    val totalSize = objects.values.sumOf { it.size }

    var bytesDownloaded = 0L

    fun <T> Collection<T>.nGroups(groups: Int): List<List<T>> {
        val result = ArrayList<MutableList<T>>(groups)
        withIndex().forEach { (i, value) ->
            result.getOrNull(i % groups)?.apply {
                add(value)
            } ?: result.add(mutableListOf(value))
        }

        return result
    }

    val assetPaths = objects
        .entries
        .nGroups(10)
        .mapAsync { window ->
            window
                .forEach { entry ->
                    val (name, asset) = entry
                    val assetPath = assetsObjectsPath resolve asset.checksum.take(2) resolve asset.checksum
                    if (assetPath.make()) {
                        for (i in 1..3) {
                            val r = runCatching {
                                val url = URL("$MINECRAFT_RESOURCES_URL/${asset.checksum.take(2)}/${asset.checksum}")

                                val unverifiedResource =
                                    RemoteResource(HttpRequestBuilder().apply {
                                        url(url)
                                    })

                                unverifiedResource copyTo assetPath
                            }

                            if (i == 3 && r.isFailure) {
                                throw r.exceptionOrNull()!!
                            } else if (r.isSuccess) {
                                break
                            } else {
                                delay(100)
                            }
                        }

                        bytesDownloaded += asset.size

                        logger.info(
                            "Downloaded asset: '$name', ${floor((bytesDownloaded.toDouble() / totalSize) * 100).toInt()}% done. ${
                                convertBytesToPrettyString(
                                    bytesDownloaded
                                )
                            }/${
                                convertBytesToPrettyString(
                                    totalSize
                                )
                            }"
                        )
                    }
                }
        }

    assetPaths
}

internal fun convertBytesToPrettyString(bytes: Long): String {
    val units = arrayOf("B", "KB", "MB", "GB", "TB")

    if (bytes == 0L) {
        return "0 B"
    }

    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()

    return String.format("%.1f %s", bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

internal fun convertMillisToTimeSpan(millis: Long): String {
    var remainingMillis = millis

    val days = remainingMillis / (24 * 60 * 60 * 1000)
    remainingMillis %= (24 * 60 * 60 * 1000)

    val hours = remainingMillis / (60 * 60 * 1000)
    remainingMillis %= (60 * 60 * 1000)

    val minutes = remainingMillis / (60 * 1000)
    remainingMillis %= (60 * 1000)

    val seconds = remainingMillis / 1000

    return listOf(
        days to "day",
        hours to "hour",
        minutes to "minute",
        seconds to "second"
    ).filter {
        it.first != 0L
    }.joinToString(separator = ", ") { (value, unit) ->
        "$value $unit${if (value != 1L) "s" else ""}"
    }.takeIf { it.isNotEmpty() } ?: "0 seconds"
}