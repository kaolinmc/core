package com.kaolinmc.core.minecraft.util

import com.kaolinmc.archives.ArchiveReference
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.name
import kotlin.sequences.forEach

public fun ArchiveReference.write(path: Path) {
    val temp = Files.createTempFile(path.name, "jar")

    JarOutputStream(FileOutputStream(temp.toFile())).use { target ->
        reader.writeAll(target)
    }

    Files.copy(temp, path, StandardCopyOption.REPLACE_EXISTING)
}

public fun ArchiveReference.write(): ByteArray {
    val out = ByteArrayOutputStream()
    JarOutputStream(out).use { target ->
        reader.writeAll(target)
    }

    return out.toByteArray()
}

private fun ArchiveReference.Reader.writeAll(
    stream: JarOutputStream,
) {
    entries().forEach { e ->
        val entry = JarEntry(e.name)

        stream.putNextEntry(entry)

        val eIn = e.open()

        //Stolen from https://stackoverflow.com/questions/1281229/how-to-use-jaroutputstream-to-create-a-jar-file
        val buffer = ByteArray(1024)

        while (true) {
            val count: Int = eIn.read(buffer)
            if (count == -1) break

            stream.write(buffer, 0, count)
        }

        stream.closeEntry()
    }
}