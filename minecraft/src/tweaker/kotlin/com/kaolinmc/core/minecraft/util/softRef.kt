package com.kaolinmc.core.minecraft.util

import com.kaolinmc.boot.loader.ClassProvider
import com.kaolinmc.boot.loader.ResourceProvider
import java.lang.ref.WeakReference
import java.net.URL

public class WeakReferenceClassProvider(
    private val delegate: WeakReference<ClassProvider>
) : ClassProvider {
    public constructor(delegate: ClassProvider) : this(WeakReference(delegate))

    override val packages: Set<String> = delegate.get()?.packages ?: emptySet()

    override fun findClass(name: String): Class<*>? {
        return delegate.get()?.findClass(name)
    }
}

public class WeakReferenceResourceProvider(
    private val delegate: WeakReference<ResourceProvider>
) : ResourceProvider {
    public constructor(delegate: ResourceProvider) : this(WeakReference(delegate))

    override fun findResources(name: String): Sequence<URL> {
        return delegate.get()?.findResources(name) ?: emptySequence()
    }
}