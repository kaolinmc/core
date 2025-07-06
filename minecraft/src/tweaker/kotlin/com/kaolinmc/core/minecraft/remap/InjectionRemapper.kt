package com.kaolinmc.core.minecraft.remap

import com.kaolinmc.mixin.engine.operation.OperationData
import org.objectweb.asm.tree.ClassNode

public interface InjectionRemapper<T: OperationData>{
    public val type: Class<T>

    public fun isTarget(
        data: T,
        node: ClassNode,
    ): Boolean

    public fun remap(
        data: T,
        destination: ClassNode,

        context: MappingContext,
    ) : T


//    public abstract fun remap(
//        annotation: AnnotationNode,
//
//        // With slashes
//        destClass: String,
//
//        mappings: ArchiveMapping,
//        source: String,
//        target: String,
//    ): T

    // TODO this will not preserve order on annotations
//    override fun remap(
//        mappings: ArchiveMapping,
//        inheritanceTree: ClassInheritanceTree,
//        source: String,
//        target: String
//    ): TransformerConfig = TransformerConfig.of {
//        transformClass { classNode ->
//            val mixinAnno = (classNode.visibleAnnotations ?: listOf()).find {
//                it.desc == Type.getType(Mixin::class.java).descriptor
//            }
//            if (mixinAnno != null) {
//                val annotations : List<AnnotationProcessor.AnnotationElement> = annotationProcessor.process(classNode, annotation)
//
//                fun <A : Annotation> AnnotationVisitor.visit(annotation: A) {
//                    annotation::class.java.declaredFields.forEach {
//                        val value = it.apply {
//                            isAccessible = true
//                        }.get(annotation)
//                        val name = it.name
//
//                        when (value) {
//                            is Annotation -> {
//                                val visitedAnno = visitAnnotation(name, Type.getType(value::class.java).descriptor)
//                                visitedAnno.visit(annotation)
//                            }
//
//                            is Enum<*> -> {
//                                // TODO is this right?
//                                visitEnum(name, Type.getType(value::class.java).descriptor, value.name)
//                            }
//                            // TODO support for arrays? It appears that the following 3 lines will do this but im not sure
//                            else -> {
//                                visit(name, value)
//                            }
//                        }
//                    }
//                }
//
//                val annotationType = Type.getType(annotation)
//
//                annotations
//                    .asSequence()
//                    .map {
//                        val remappedAnnotation = remap(
//                            it.annotation,
//                            (createValueMap(mixinAnno.values ?: listOf())["value"] as Type).internalName,
//                            mappings,
//                            source,
//                            target,
//                        )
//
//                        val node = AnnotationNode(annotationType.descriptor)
//
//                        node.visit(remappedAnnotation)
//
//                        node to it.target
//                    }
//                    .groupBy { it.second }
//                    .mapValues { it.value.map { it.first } }
//                    .forEach { (target, it: List<AnnotationNode>) ->
//                        when (target.elementType) {
//                            ElementType.CLASS -> {
//                                (target.classNode.visibleAnnotations ?: mutableListOf()).removeIf {
//                                    it.desc == annotationType.descriptor
//                                }
//                                (target.classNode.visibleAnnotations).addAll(it)
//                            }
//
//                            ElementType.METHOD -> {
//                                (target.methodNode.visibleAnnotations ?: mutableListOf()).removeIf {
//                                    it.desc == annotationType.descriptor
//                                }
//                                (target.methodNode.visibleAnnotations ?: mutableListOf()).addAll(it)
//                            }
//
//                            ElementType.FIELD -> {
//                                (target.fieldNode.visibleAnnotations ?: mutableListOf()).removeIf {
//                                    it.desc == annotationType.descriptor
//                                }
//                                (target.fieldNode.visibleAnnotations ?: mutableListOf()).addAll(it)
//                            }
//
//                            ElementType.PARAMETER -> {
//                                (target.methodNode.visibleParameterAnnotations
//                                    ?: arrayOf())[target.parameter].removeIf {
//                                    it.desc == annotationType.descriptor
//                                }
//                                (target.methodNode.visibleParameterAnnotations
//                                    ?: arrayOf())[target.parameter].addAll(it)
//                            }
//                        }
//                    }
//            }
//
//            classNode
//        }
//    }
}