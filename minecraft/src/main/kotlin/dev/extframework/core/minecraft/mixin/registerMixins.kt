package dev.extframework.core.minecraft.mixin

import dev.extframework.common.util.runCatching
import dev.extframework.core.instrument.instrumentAgentsAttrKey
import dev.extframework.core.minecraft.environment.engineAttrKey
import dev.extframework.core.minecraft.environment.injectionRemappersAttrKey
import dev.extframework.core.minecraft.environment.redefinitionAttrKey
import dev.extframework.core.minecraft.internal.remap.CodeInjectionRemapper
import dev.extframework.core.minecraft.internal.remap.MappingMixinInjector
import dev.extframework.core.minecraft.internal.remap.MixinMappingManager
import dev.extframework.core.minecraft.remap.MappingManager
import dev.extframework.mixin.MixinEngine
import dev.extframework.mixin.RedefinitionFlags
import dev.extframework.mixin.api.ClassReference
import dev.extframework.mixin.api.InjectCode
import dev.extframework.mixin.api.InjectMethod
import dev.extframework.mixin.api.InstructionSelector
import dev.extframework.mixin.internal.inject.MixinInjector
import dev.extframework.mixin.internal.inject.impl.code.InstructionInjector
import dev.extframework.mixin.internal.inject.impl.method.MethodInjector
import dev.extframework.tooling.api.environment.*
import dev.extframework.tooling.api.extension.partition.ExtensionPartitionContainer
import org.objectweb.asm.Type

public fun registerMixins(environment: ExtensionEnvironment) {
    // Injection remapping

    val injectionRemappers = MutableObjectSetAttribute(injectionRemappersAttrKey)
    injectionRemappers.add(CodeInjectionRemapper())
    environment += injectionRemappers

    // Mixin Mapping Manager
    val mixinMappingManager = MixinMappingManager(
        injectionRemappers,
        environment[MappingManager].extract(),
    )

    // Redefinition
    var redefinitionFlags = environment[redefinitionAttrKey].getOrNull()?.let {
        RedefinitionFlags.valueOf(it.value)
    } ?: RedefinitionFlags.ONLY_INSTRUCTIONS

    // Type provision
    var typeProvider: (ClassReference) -> Class<*>? = { ref ->
        environment.archiveGraph.nodes()
            .filterIsInstance<ExtensionPartitionContainer<*, *>>()
            .filter { it.metadata.name == "tweaker" }
            .firstNotNullOfOrNull {
                runCatching(ClassNotFoundException::class) {
                    it.handle?.classloader?.loadClass(ref.name)
                }
            }
    }

    // Raw Injectors
    val methodInjector = MethodInjector(redefinitionFlags)
    var instructionInjector = InstructionInjector(methodInjector) {
        typeProvider(ClassReference(it)) as Class<out InstructionSelector>
    }

    val engine = MixinEngine(
        redefinitionFlags,
        typeProvider = typeProvider,
        injectors = mapOf<Type, MixinInjector<*>>(
            Type.getType(InjectMethod::class.java) to MappingMixinInjector(
                methodInjector,
                mixinMappingManager
            ),
            Type.getType(InjectCode::class.java) to MappingMixinInjector(
                instructionInjector,
                mixinMappingManager
            )
        )
    )
    environment += ValueAttribute(engine, engineAttrKey)

    // Instrumentation agents
    val instrumentAgents = environment[instrumentAgentsAttrKey].extract()
    instrumentAgents.add(DefaultMixinSubsystem(
        engine,
        mixinMappingManager
    ))
}