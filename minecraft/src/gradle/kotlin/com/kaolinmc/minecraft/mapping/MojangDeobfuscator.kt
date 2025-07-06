package com.kaolinmc.minecraft.mapping

import com.durganmcbroom.resources.Resource
import com.durganmcbroom.resources.toByteArray
import com.kaolinmc.archive.mapper.ArchiveMapping
import com.kaolinmc.archive.mapper.MappingsProvider
import com.kaolinmc.archive.mapper.parsers.proguard.ProGuardMappingParser
import com.kaolinmc.boot.store.CachingDataStore
import com.kaolinmc.boot.store.DataAccess
import com.kaolinmc.boot.store.DataStore
import com.kaolinmc.common.util.copyTo
import com.kaolinmc.common.util.resolve
import com.kaolinmc.common.util.toResource
import com.kaolinmc.launchermeta.handler.clientMappings
import com.kaolinmc.launchermeta.handler.loadVersionManifest
import com.kaolinmc.launchermeta.handler.metadata
import com.kaolinmc.launchermeta.handler.parseMetadata
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

//public class MojangDeobfuscator(
//    path: Path
//) : MinecraftDeobfuscator {
//    override val provider: MappingsProvider = MojangMappingProvider(path)
//    override val obfuscatedNamespace: String = "mojang:obfuscated"
//    override val deobfuscatedNamespace: String = "mojang:deobfuscated"
//
//    override fun getName(): String {
//        return "mojang"
//    }
//}
//
//internal class MojangMappingProvider(
//    private val mappingStore: DataStore<String, Resource>
//) : MappingsProvider {
//    companion object {
//        const val REAL_TYPE: String = "mojang:deobfuscated"
//        const val FAKE_TYPE: String = "mojang:obfuscated"
//    }
//
//    constructor(path: Path) : this(CachingDataStore(MojangMappingAccess(path)))
//
//    override val namespaces: Set<String> = setOf(REAL_TYPE, FAKE_TYPE)
//
//    override fun forIdentifier(identifier: String): ArchiveMapping = runBlocking {
//        val mappingData = mappingStore[identifier] ?: run {
//            val manifest = loadVersionManifest()
//            val version = manifest.find(identifier)
//                ?: throw IllegalArgumentException("Unknown minecraft version for mappings: '$identifier'")
//            val m = parseMetadata(version.metadata().getOrThrow()).getOrThrow().clientMappings().getOrThrow()
//            mappingStore.put(identifier, m)
//            m
//        }
//
//        ProGuardMappingParser(FAKE_TYPE, REAL_TYPE).parse(ByteArrayInputStream(mappingData.open().toByteArray()))
//    }
//}
//
//private class MojangMappingAccess(
//    private val path: Path
//) : DataAccess<String, Resource> {
//    override fun read(key: String): Resource? {
//        val versionPath = path resolve "client-mappings-$key.json"
//
//        if (!versionPath.exists()) return null
//
//        return versionPath.toUri().toResource()
//    }
//
//    override fun write(key: String, value: Resource) {
//        val versionPath = path resolve "client-mappings-$key.json"
//        versionPath.deleteIfExists()
//
//        runBlocking {
//            value.copyTo(versionPath)
//        }
//    }
//}