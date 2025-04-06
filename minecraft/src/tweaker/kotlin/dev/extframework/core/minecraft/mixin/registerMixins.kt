package dev.extframework.core.minecraft.mixin

import dev.extframework.common.util.runCatching
import dev.extframework.core.app.api.ApplicationTarget
import dev.extframework.core.instrument.InstrumentedApplicationTarget
import dev.extframework.core.instrument.instrumentAgentsAttrKey
import dev.extframework.core.minecraft.environment.engineAttrKey
import dev.extframework.core.minecraft.environment.injectionRemappersAttrKey
import dev.extframework.core.minecraft.environment.redefinitionAttrKey
import dev.extframework.core.minecraft.internal.remap.CodeInjectionRemapper
import dev.extframework.core.minecraft.internal.remap.MappingClassTransformer
import dev.extframework.core.minecraft.internal.remap.MethodInjectionRemapper
import dev.extframework.core.minecraft.internal.remap.MixinMappingManager
import dev.extframework.core.minecraft.remap.MappingManager
import dev.extframework.mixin.MixinEngine
import dev.extframework.mixin.RedefinitionFlags
import dev.extframework.mixin.api.ClassReference
import dev.extframework.mixin.api.InjectCode
import dev.extframework.mixin.api.InstructionSelector
import dev.extframework.mixin.engine.impl.code.InstructionInjectionParser
import dev.extframework.mixin.engine.impl.code.InstructionInjector
import dev.extframework.mixin.engine.impl.method.MethodInjector
import dev.extframework.tooling.api.environment.*
import dev.extframework.tooling.api.extension.partition.ExtensionPartitionContainer
import org.objectweb.asm.Type

public fun registerMixins(environment: ExtensionEnvironment) {
    // Injection remapping
    val injectionRemappers = MutableObjectSetAttribute(injectionRemappersAttrKey)
    injectionRemappers.add(CodeInjectionRemapper())
    injectionRemappers.add(MethodInjectionRemapper())
    environment += injectionRemappers

    // Mixin Mapping Manager
    val mixinMappingManager = MixinMappingManager(
        injectionRemappers,
        environment[MappingManager].extract(),
    )

    // Redefinition
    var redefinitionFlags: RedefinitionFlags = environment[redefinitionAttrKey].getOrNull()?.let {
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

    environment += ValueAttribute(engine, engineAttrKey)

    // Instrumentation agents
    val instrumentAgents = environment[instrumentAgentsAttrKey].extract()
    instrumentAgents.add(
        DefaultMixinSubsystem(
            engine,
            mixinMappingManager,
            environment[ApplicationTarget].extract() as InstrumentedApplicationTarget,
        )
    )
}