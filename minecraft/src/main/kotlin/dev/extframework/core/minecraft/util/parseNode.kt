package dev.extframework.core.minecraft.util

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.InputStream

public fun InputStream.parseNode(): ClassNode {
    val node = ClassNode()
    ClassReader(this).accept(node, ClassReader.EXPAND_FRAMES)
    return node
}