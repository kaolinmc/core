package dev.extframework.minecraft.mapping

import dev.extframework.archive.mapper.ArchiveMapping
import dev.extframework.archive.mapper.MappingNodeContainerImpl
import dev.extframework.archive.mapper.MappingValueContainerImpl
import dev.extframework.archive.mapper.MappingsProvider
import java.nio.file.Path

//public object MinecraftMappings {
//    @JvmStatic
//    public lateinit var mojang : MinecraftDeobfuscator
//        private set
//    @JvmStatic
//    public lateinit var none: MinecraftDeobfuscator
//        private set
//
//    internal fun setup(path: Path) {
//        mojang = MojangDeobfuscator(path)
//        none = object : MinecraftDeobfuscator {
//            override val provider: MappingsProvider = object : MappingsProvider {
//                override val namespaces: Set<String> = setOf("mojang:obfuscated")
//
//                override fun forIdentifier(identifier: String): ArchiveMapping {
//                    return ArchiveMapping(
//                        namespaces,MappingValueContainerImpl(HashMap()), MappingNodeContainerImpl(setOf())
//                    )
//                }
//            }
//            override val obfuscatedNamespace: String = "mojang:obfuscated"
//            override val deobfuscatedNamespace: String = "mojang:obfuscated"
//
//            override fun getName(): String = "none"
//        }
//    }
//}