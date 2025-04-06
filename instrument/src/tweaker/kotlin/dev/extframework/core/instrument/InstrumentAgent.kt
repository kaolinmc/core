package dev.extframework.core.instrument

import org.objectweb.asm.tree.ClassNode

public interface InstrumentAgent {
    public fun transformClass(name: String, node: ClassNode?) : ClassNode?
}