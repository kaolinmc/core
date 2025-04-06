package dev.extframework.minecraft

import dev.extframework.core.minecraft.api.MappingNamespace
import dev.extframework.gradle.api.EvaluatingDependency
import dev.extframework.gradle.api.ExtframeworkExtension
import dev.extframework.gradle.api.MutablePartitionRuntimeModel
import dev.extframework.gradle.api.PartitionDependencyHandler
import dev.extframework.gradle.api.PartitionHandler
import dev.extframework.minecraft.task.`GenerateMinecraftSource`
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.tasks.SourceSet

public class MinecraftPartitionHandler(
    private val project: Project,
    private val partition: MutablePartitionRuntimeModel,
    sourceSet: SourceSet,
    public val extFrameworkExtension: ExtframeworkExtension, configure: (() -> Unit) -> Unit,
) : PartitionHandler<MinecraftPartitionDependencyHandler>(project, partition, sourceSet, configure) {
    init {
        sourceSet.jarTaskName
    }

    override val dependencies: MinecraftPartitionDependencyHandler by lazy {
        MinecraftPartitionDependencyHandler(
            project.dependencies,
            sourceSet,
            project,
            mappings,
        ) {
            partition.dependencies.add(it)
        }
    }

    public var mappings: MappingNamespace
        get() {
            return MappingNamespace.parse(
                partition.options.getting("mappingNS").orNull
                    ?: throw Exception("Please set your mappings provider in partition: '${sourceSet.name}'")
            )
        }
        set(value) {
            partition.options.put("mappingNS", value.identifier)
        }
    public var supportedVersions: List<String>
        get() {
            return partition.options.getting("versions").orNull?.split(",") ?: listOf()
        }
        set(value) {
            partition.options.put("versions", value.joinToString(separator = ","))
        }
    public var entrypoint: String?
        get() = partition.options.getting("entrypoint").orNull
        set(value) {
            if (value != null) partition.options.put("entrypoint", value)
        }

    public fun supportVersions(vararg versions: String) {
        supportedVersions += versions.toList()
    }
}

public class MinecraftPartitionDependencyHandler(
    delegate: DependencyHandler,
    sourceSet: SourceSet,

    private val project: Project,
    private val mappingsType: MappingNamespace,
    addDependency: (EvaluatingDependency) -> Unit
) : PartitionDependencyHandler(
    delegate,
    sourceSet,
    addDependency
) {
    public val extframework: ExtframeworkExtension = project.extensions.getByType(ExtframeworkExtension::class.java)

    public fun minecraft(version: String) {
        val taskName = "generateMinecraft${version}Sources"
        val task =
            project.tasks.findByName(taskName) ?: project.tasks.create(taskName, GenerateMinecraftSource::class.java) {
                it.namespace.set(mappingsType)
                it.minecraftVersion.set(version)
            }

        delegate.add(
            sourceSet.implementationConfigurationName,
            project.files(task.outputs.files.asFileTree).apply {
                builtBy(task)
            }
        )
    }
}
