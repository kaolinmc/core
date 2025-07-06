package com.kaolinmc.core.minecraft.internal

import com.durganmcbroom.resources.Resource
import com.kaolinmc.boot.store.DataAccess
import com.kaolinmc.common.util.copyTo
import com.kaolinmc.common.util.resolve
import com.kaolinmc.common.util.toResource
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

internal class MojangMappingAccess(
    private val path: Path
) : DataAccess<String, Resource> {
    override fun read(key: String): Resource? {
        val versionPath = path resolve "client-mappings-$key.json"

        if (!versionPath.exists()) return null

        return versionPath.toUri().toResource()
    }

    override fun write(key: String, value: Resource) {
        val versionPath = path resolve "client-mappings-$key.json"
        versionPath.deleteIfExists()

        runBlocking {
            value.copyTo(versionPath)
        }
    }
}