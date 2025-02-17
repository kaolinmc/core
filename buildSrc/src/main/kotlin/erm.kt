import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.artifacts.mvnsettings.DefaultLocalMavenRepositoryLocator
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.DefaultMavenLocalArtifactRepository
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.io.Serializable
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.Path

abstract class GenerateErm : DefaultTask() {
    @get:Input
    @get:Optional
    abstract val version: Property<String>

    @get:Input
    abstract val parents: ListProperty<ExtensionParent>

    @get:Input
    abstract val partitions: ListProperty<PartitionRuntimeModel>

    @get:Input
    val includeMavenLocal: Property<Boolean> = project.objects.property(Boolean::class.java).convention(false)

    @get:OutputFile
    val outputFile: RegularFileProperty = project.objects.fileProperty().convention(
        project.layout.buildDirectory.file("libs/erm.json")
    )

    fun parents(
        action: Action<MutableList<Project>>
    ) {
        val list = ArrayList<Project>()
        action.execute(list)
        parents.addAll(project.provider {
            list.map {
                ExtensionParent(
                    it.group as String,
                    it.name,
                    it.version as String
                )
            }
        })
    }

    fun partitions(
        action: Action<MutableList<GeneratePrm>>
    ) {
        val list: ArrayList<GeneratePrm> = ArrayList()
        action.execute(list)

        dependsOn(list)

        partitions.addAll(project.provider {
            list.map {
                it.prm.get()
            }
        })
    }

    @TaskAction
    fun generate() {
        val repositories = arrayListOf(
            mapOf(
                "type" to "default",
                "location" to "https://repo.extframework.dev/registry"
            )
        )

        if (includeMavenLocal.get()) repositories.add(
            0,
            mapOf(
                "type" to "local",
                "location" to Paths.get(project.repositories.mavenLocal().url).toString()
            )
        )

        val model = ExtensionRuntimeModel(
            2,
            project.group as String,
            project.name,
            version.orNull ?: project.version as String,
            repositories,
            parents.get().toSet(),
            partitions.get().toSet()
        )

        outputFile.get().asFile.parentFile.mkdirs()
        outputFile.get().asFile.writeBytes(jacksonObjectMapper().writeValueAsBytes(model))
    }
}


data class ExtensionRuntimeModel(
    val apiVersion: Int,
    val groupId: String,
    val name: String,
    val version: String,

    val repositories: List<Map<String, String>> = ArrayList(),
    val parents: Set<ExtensionParent> = HashSet(),

    public val partitions: Set<PartitionRuntimeModel>
)

public data class ExtensionParent(
    val group: String,
    val extension: String,
    val version: String
) : Serializable {
}