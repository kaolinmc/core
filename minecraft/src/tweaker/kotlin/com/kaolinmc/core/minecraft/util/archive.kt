package com.kaolinmc.core.minecraft.util

import com.kaolinmc.archives.ArchiveHandle
import com.kaolinmc.archives.ArchiveReference
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.name

private class THIS

public fun emptyArchiveHandle(): ArchiveHandle = object : ArchiveHandle {
    override val classloader: ClassLoader = THIS::class.java.classLoader
    override val name: String? = null
    override val packages: Set<String> = setOf()
    override val parents: Set<ArchiveHandle> = setOf()
}

public fun emptyArchiveReference(
    location: URI = URI("archive:empty")
): ArchiveReference = object : ArchiveReference {
    private val entries: MutableMap<String, () -> ArchiveReference.Entry> = HashMap()
    override val isClosed: Boolean = false
    override val location: URI = location
    override val modified: Boolean = entries.isNotEmpty()
    override val name: String? = null
    override val reader: ArchiveReference.Reader = object : ArchiveReference.Reader {
        override fun entries(): Sequence<ArchiveReference.Entry> {
            return entries.values.asSequence().map { it() }
        }

        override fun of(name: String): ArchiveReference.Entry? {
            return entries[name]?.invoke()
        }
    }
    override val writer = object : ArchiveReference.Writer {
        override fun put(entry: ArchiveReference.Entry) {
            entries[entry.name] = { entry }
        }

        override fun remove(name: String) {
            entries.remove(name)
        }

    }

    override fun close() {}
}
