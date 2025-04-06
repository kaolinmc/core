package dev.extframework.minecraft

import com.durganmcbroom.artifact.resolver.ArtifactMetadata
import com.durganmcbroom.jobs.Job
import com.durganmcbroom.jobs.job
import dev.extframework.archives.ArchiveHandle
import dev.extframework.boot.archive.ArchiveAccessTree
import dev.extframework.boot.archive.ArchiveTarget
import dev.extframework.boot.archive.ClassLoadedArchiveNode
import dev.extframework.core.app.api.ApplicationDescriptor
import dev.extframework.core.minecraft.api.MinecraftAppApi
import dev.extframework.core.minecraft.util.emptyArchiveHandle
import dev.extframework.gradle.api.GradleEntrypoint
import dev.extframework.tooling.api.environment.ExtensionEnvironment
import org.gradle.api.Project
import java.nio.file.Path
import kotlin.io.path.Path

public class MinecraftGradleEntrypoint : GradleEntrypoint {
    override fun apply(project: Project) {
//        val extension = project.rootProject.extensions.getByType(ExtensionWorker::class.java)
//        val mappers = project.extensions.create(
//            "mappers", MappersExtension::class.java, project.objects.namedDomainObjectSet(
//                MinecraftDeobfuscator::class.java
//            )
//        )
//        mappers.add(MojangDeobfuscator(
//            extension.dataDir resolve "mappings"
//        ))
//        project.tasks.maybeCreate("setup").apply {
//            dependsOn(project.tasks.withType(GenerateMinecraftSource::class.java))
//        }
    }

    override fun setup(environment: ExtensionEnvironment): Job<Unit> = job {
        println("Setting up rn")
        val descriptor = ApplicationDescriptor("setup", "app", "1", null)
        environment += object : MinecraftAppApi() {
            override val node: ClassLoadedArchiveNode<ApplicationDescriptor> = object : ClassLoadedArchiveNode<ApplicationDescriptor> {
                override val handle: ArchiveHandle = emptyArchiveHandle()
                override val access: ArchiveAccessTree = object : ArchiveAccessTree {
                    override val descriptor: ArtifactMetadata.Descriptor = descriptor
                    override val targets: List<ArchiveTarget> = listOf()
                }

                override val descriptor: ApplicationDescriptor = descriptor
            }
            override val path: Path = Path("")
            override val gameDir: Path= Path("")
            override val gameJar: Path= Path("")
            override val classpath: List<Path> = listOf()
            override val version: String = descriptor.version
        }
    }
}