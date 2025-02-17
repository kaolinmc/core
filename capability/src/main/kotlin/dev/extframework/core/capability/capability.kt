package dev.extframework.core.capability

public interface Capability {
    public fun invokeUnsafe(vararg args: Any?): Any?

    public data class Reference<T : Capability>(
        val name: String,
        val type: Class<T>,
        val capabilities: Capabilities,
    ) {
        public val call: T
            @JvmName("caller")
            get() = capabilities[this]

        public operator fun plusAssign(capability: T) {
            capabilities.register(this, capability)
        }
    }
}

// -------------------------------------------------------------
//                  Basic Capability providers
// -------------------------------------------------------------

public fun interface Capability0<R> : Capability {
    public operator fun invoke(): R

    override fun invokeUnsafe(vararg args: Any?): Any? {
        return invoke()
    }
}

public fun interface Capability1<P1, R> : Capability {
    public operator fun invoke(p1: P1): R

    override fun invokeUnsafe(vararg args: Any?): Any? {
        return invoke(args[0] as P1)
    }
}

public fun interface Capability2<P1, P2, R> : Capability {
    public operator fun invoke(p1: P1, p2: P2): R

    override fun invokeUnsafe(vararg args: Any?): Any? {
        return invoke(args[0] as P1, args[0] as P2)
    }
}

public fun interface Capability3<P1, P2, P3, R> : Capability {
    public operator fun invoke(p1: P1, p2: P2, p3: P3): R

    override fun invokeUnsafe(vararg args: Any?): Any? {
        return invoke(args[0] as P1, args[0] as P2, args[0] as P3)
    }
}

public fun interface Capability4<P1, P2, P3, P4, R> : Capability {
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4): R

    override fun invokeUnsafe(vararg args: Any?): Any? {
        return invoke(
            args[0] as P1,
            args[0] as P2,
            args[0] as P3,
            args[0] as P4
        )
    }
}