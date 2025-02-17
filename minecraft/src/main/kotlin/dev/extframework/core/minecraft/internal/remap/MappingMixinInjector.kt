package dev.extframework.core.minecraft.internal.remap

import dev.extframework.common.util.LazyMap
import dev.extframework.core.minecraft.api.MappingNamespace
import dev.extframework.core.minecraft.remap.InjectionRemapper
import dev.extframework.core.minecraft.remap.MappingContext
import dev.extframework.core.minecraft.remap.MappingManager
import dev.extframework.mixin.annotation.AnnotationTarget
import dev.extframework.mixin.api.ClassReference
import dev.extframework.mixin.api.ClassReference.Companion.ref
import dev.extframework.mixin.internal.inject.InjectionData
import dev.extframework.mixin.internal.inject.MixinInjector
import dev.extframework.tooling.api.environment.MutableObjectSetAttribute
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode

public class MixinMappingManager(
    private val remappers: MutableObjectSetAttribute<InjectionRemapper<*>>,
    private val mappingManager: MappingManager
) {
    private val tags = HashMap<ClassReference, MappingNamespace>()

    public fun tag(
        cls: ClassReference,
        source: MappingNamespace,
    ) {
        if (tags.containsKey(cls)) {
            throw Exception("Two classes were registered for the same mixin mapping context! This is a flaw in extframework, please report. '$cls'")
        }

        tags[cls] = source
    }

    public fun <T : InjectionData> remap(
        data: T,
        destination: ClassNode,
        mixinClass: ClassReference,
    ): T {
        val tag = tags[mixinClass] ?: return data

        return (remappers.find {
            it.type.isInstance(data)
        } as? InjectionRemapper<T>)?.remap(
            data,
            destination,
            mappingManager[tag],
        ) ?: data
    }
}

public open class MappingMixinInjector<T : InjectionData>(
    protected val delegate: MixinInjector<T>,
    protected val manager: MixinMappingManager,
    public val dataSource: MutableMap<InjectionData, ClassReference> = HashMap<InjectionData, ClassReference>()
) : MixinInjector<T> {
    override fun parse(
        target: AnnotationTarget,
        annotation: AnnotationNode
    ): T {
        val data = delegate.parse(target, annotation)
        dataSource[data] = target.classNode.ref()
        return data
    }

    override fun inject(
        node: ClassNode,
        helper: MixinInjector.InjectionHelper<T>
    ) {
        delegate.inject(node, object : MixinInjector.InjectionHelper<T> {
            private val remapped = LazyMap<T, T> {
                dataSource[it]?.let { source ->
                    manager.remap(
                        it,
                        node,
                        source
                    )
                } ?: it
            }

            // FIXME: BUG: this method will attempt to remap all data that belongs to this
            //    injector, however in some cases this isn't possible (Eg. a code injection requires
            //    a class node to remap, and if it is not the correct one it will not work). Current
            //    work around is to lazy evaluate and only remap applicable injections.
            override val allData: Set<T> by lazy {
                val applicable = helper.applicable().toSet()
                helper.allData.mapTo(HashSet()) {
                    it.takeUnless { applicable.contains(it) } ?: remapped[it]!!
                }
            }

            override fun applicable(): List<T> {
                return helper.applicable().map { remapped[it]!! }
            }

            override fun <T2 : InjectionData> inject(
                node: ClassNode,
                injector: MixinInjector<T2>,
                data: List<T2>
            ) {
                val wrappedInjector = MappingMixinInjector(injector, manager)

                helper.inject(node, wrappedInjector, data)
            }
        })
    }
}