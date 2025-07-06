package com.kaolinmc.core.minecraft.mixin

import com.kaolinmc.common.util.runCatching
import com.kaolinmc.core.app.api.ApplicationTarget
import com.kaolinmc.core.instrument.InstrumentedApplicationTarget
import com.kaolinmc.core.instrument.instrumentAgentsAttrKey
import com.kaolinmc.core.minecraft.environment.engineAttrKey
import com.kaolinmc.core.minecraft.environment.injectionRemappersAttrKey
import com.kaolinmc.core.minecraft.environment.redefinitionAttrKey
import com.kaolinmc.core.minecraft.internal.remap.CodeInjectionRemapper
import com.kaolinmc.core.minecraft.internal.remap.MappingClassTransformer
import com.kaolinmc.core.minecraft.internal.remap.MethodInjectionRemapper
import com.kaolinmc.core.minecraft.internal.remap.MixinMappingManager
import com.kaolinmc.core.minecraft.remap.MappingManager
import com.kaolinmc.mixin.MixinEngine
import com.kaolinmc.mixin.RedefinitionFlags
import com.kaolinmc.mixin.api.ClassReference
import com.kaolinmc.mixin.api.InjectCode
import com.kaolinmc.mixin.api.InstructionSelector
import com.kaolinmc.mixin.engine.impl.code.InstructionInjectionParser
import com.kaolinmc.mixin.engine.impl.code.InstructionInjector
import com.kaolinmc.mixin.engine.impl.method.MethodInjector
import com.kaolinmc.tooling.api.ExtensionLoader
import com.kaolinmc.tooling.api.environment.*
import com.kaolinmc.tooling.api.extension.partition.ExtensionPartitionContainer
import org.objectweb.asm.Type

public fun registerMixins(environment: ExtensionEnvironment) {
    // Injection remapping
    val injectionRemappers = MutableSetAttribute(injectionRemappersAttrKey)
    injectionRemappers.add(CodeInjectionRemapper())
    injectionRemappers.add(MethodInjectionRemapper())
    environment += injectionRemappers

    // Mixin Mapping Manager
    val mixinMappingManager = MixinMappingManager(
        injectionRemappers,
        environment[MappingManager],
    )

    // Redefinition
    var redefinitionFlags: RedefinitionFlags = environment.find(redefinitionAttrKey)?.let {
        RedefinitionFlags.valueOf(it.value)
    } ?: RedefinitionFlags.ONLY_INSTRUCTIONS

    // Type provision
    var typeProvider: (ClassReference) -> Class<*>? = { ref ->
        environment[ExtensionLoader].graph.nodes.values
            .map { it.value }
            .filterIsInstance<ExtensionPartitionContainer<*, *>>()
            .filter { it.metadata.name == "tweaker" }
            .firstNotNullOfOrNull {
                runCatching(ClassNotFoundException::class) {
                    it.handle?.classloader?.loadClass(ref.name)
                }
            }
    }

    val engine = MixinEngine(
        typeProvider
    )

    // Registering transformers / parsers with mapping
    val methodInjector = MappingClassTransformer(MethodInjector(redefinitionFlags), mixinMappingManager)
    val instructionInjector = MappingClassTransformer(InstructionInjector(methodInjector), mixinMappingManager)

    engine.parsers[Type.getType(InjectCode::class.java)] = InstructionInjectionParser(instructionInjector) {
        typeProvider(ClassReference(it)) as Class<out InstructionSelector>
    }
    engine.transformers.add(methodInjector)
    engine.transformers.add(instructionInjector)

    environment += ValueAttribute(engineAttrKey,engine)

    // Instrumentation agents
    val instrumentAgents = environment[instrumentAgentsAttrKey]
    instrumentAgents.add(
        DefaultMixinSubsystem(
            engine,
            mixinMappingManager,
            environment[ApplicationTarget] as InstrumentedApplicationTarget,
        )
    )
}