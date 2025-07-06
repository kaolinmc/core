package com.kaolinmc.core.minecraft.internal.remap

import com.kaolinmc.core.minecraft.remap.InjectionRemapper
import com.kaolinmc.core.minecraft.remap.MappingManager
import com.kaolinmc.mixin.engine.operation.OperationData
import com.kaolinmc.mixin.engine.operation.OperationRegistry
import com.kaolinmc.mixin.engine.tag.ClassTag
import com.kaolinmc.mixin.engine.transform.ClassTransformer
import com.kaolinmc.tooling.api.environment.MutableSetAttribute
import org.objectweb.asm.tree.ClassNode

public class MixinMappingManager(
    private val remappers: MutableSetAttribute<InjectionRemapper<*>>,
    private val mappingManager: MappingManager
) {
    public fun <T : OperationData> isTarget(
        data: T,
        destination: ClassNode,
    ): Boolean {
        return (remappers.find {
            it.type.isInstance(data)
        } as? InjectionRemapper<T>)?.isTarget(
            data,
            destination
        ) == true
    }

    public fun <T : OperationData> remap(
        data: T,
        destination: ClassNode,
        tag: ClassTag,
    ): T {
        if (tag !is MappingAwareClassTag) {
            return data
        }

        return (remappers.find {
            it.type.isInstance(data)
        } as? InjectionRemapper<T>)
            ?.remap(
                data,
                destination,
                mappingManager[tag.namespace],
            ) ?: data
    }
}

public open class MappingOperationRegistry<T : OperationData>(
    protected open val delegate: OperationRegistry<T>,
    protected val manager: MixinMappingManager,
) : OperationRegistry<T> {
    protected val tracked: MutableMap<T, ClassTag> = HashMap<T, ClassTag>()
    protected val unmapped: MutableSet<T> = HashSet<T>()

    public fun remap(
        target: ClassNode,
    ) {
        val mappable = unmapped.filter {
            manager.isTarget(it, target)
        }
        unmapped.removeAll(mappable)

        val mapped = mappable.map {
            var mixinClass = tracked[it]!!
            manager.remap(it, target, mixinClass) to mixinClass
        }

        for ((data, mixinClass) in mapped) {
            delegate.register(data, mixinClass)
        }
    }

    override fun register(data: T, tag: ClassTag) {
        tracked[data] = tag
        unmapped.add(data)
    }

    override fun unregister(tag: ClassTag): List<T> {
        val result = delegate.unregister(tag)
        for (t in result) {
            tracked.remove(t)
        }
        return result
    }
}

//public open class MappingInjectionParser<T : OperationData>(
//    protected val delegate: InjectionParser<T>,
//    manager: MixinMappingManager
//) : InjectionParser<T> by delegate {
//    override val injector: MixinInjector<T> =
//
//        MappingMixinInjector(
//        delegate.injector,
//        manager,
//    )
//}

public open class MappingClassTransformer<T : OperationData>(
    public val delegate: ClassTransformer<T>,
    manager: MixinMappingManager
) : ClassTransformer<T> by delegate {
    override val registry: MappingOperationRegistry<T> = MappingOperationRegistry(delegate.registry, manager)

    override fun transform(node: ClassNode): ClassNode {
        registry.remap(node)

        return delegate.transform(node)
    }
}

//public open class MappingInjectionParser<T : OperationData>(
//    public val delegate: InjectionParser<T>,
//) : InjectionParser<T> by delegate