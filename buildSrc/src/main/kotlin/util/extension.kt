package util

import GenerateErm
import GeneratePrm
import PartitionRuntimeModel
import dev.extframework.gradle.publish.ExtensionPublishTask
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import publish.BuildBundle
import publish.GenerateMetadata
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

//private class RegisteringDelegate<T : Task, U : T>(
//    val type: KClass<U>,
//    val block: U.() -> Unit,
//    val registry: TaskContainer
//) {
//    var value: TaskProvider<U>? = null
//
//    operator fun getValue(thisRef: Any?, property: KProperty<*>): TaskProvider<U> {
//        return value ?: run {
//            println(property.name)
//            val provider = registry.register(property.name, type.java, block)
//            value = provider
//            provider
//        }
//    }
//}
//
//private fun <T : Task, C : TaskContainer, U : T> C.registering(
//    type: KClass<U>, block: U.() -> Unit
//): RegisteringDelegate<T, U> {
//    return RegisteringDelegate(type, block, this@registering)
//}

fun Project.basicExtensionInfo(
    tweakerClass: String,
    name: String,
    description: String,
    parents: Action<MutableList<Project>> = Action { }
) {
    val generateTweakerPrm = tasks.create("generateTweakerPrm", GeneratePrm::class.java) {
        sourceSetName.set("main")
        prm.set(
            PartitionRuntimeModel(
                "tweaker", "tweaker",
                options = mutableMapOf(
                    "tweaker-class" to tweakerClass
                )
            )
        )
        ignoredModules.addAll(
            ArrayList<Project>()
                .also(parents::execute)
                .map { "${it.group}:${it.name}" }
        )
    }

    val generateMetadata = tasks.create("generateMetadata", GenerateMetadata::class.java) {
        metadata {
            this.name.set(name)
            developers.add("extframework")
            this.description.set(description)
            app.set("*")
        }
    }

    val generateErm = tasks.create("generateErm", GenerateErm::class.java) {
        partitions {
            add(generateTweakerPrm)
        }
        this.parents(parents)
    }

    val buildBundle = tasks.create("buildBundle", BuildBundle::class.java) {
        partition("tweaker") {
            jar(tasks.getByName("jar"))
            prm(generateTweakerPrm)
        }

        erm.from(generateErm)
        metadata.from(generateMetadata)
    }

    val generateTestTweakerPrm = tasks.create("generateTestTweakerPrm", GeneratePrm::class.java) {
        sourceSetName.set("main")
        prm.set(
            PartitionRuntimeModel(
                "tweaker", "tweaker",
                options = mutableMapOf(
                    "tweaker-class" to tweakerClass
                )
            )
        )
        includeMavenLocal.set(true)
        ignoredModules.addAll(
            ArrayList<Project>()
                .also(parents::execute)
                .map { "${it.group}:${it.name}" }
        )
    }
//    tasks.registering(GeneratePrm::class) {
//
//    }

    val generateTestErm = tasks.create("generateTestErm", GenerateErm::class.java) {
        partitions {
            add(generateTestTweakerPrm)
        }
        includeMavenLocal.set(true)
        this.parents(parents)
    }

    extensions.getByType(PublishingExtension::class).apply {
        repositories {
            maven {
                url = uri("https://repo.extframework.dev")

                credentials {
                    password = project.properties["creds.ext.key"] as? String
                }
            }
        }
        publications {
            create("maven", MavenPublication::class.java) {
                artifact(generateTestErm.outputFile).classifier = "erm"
                artifact(tasks.named("jar")).classifier = "tweaker"
            }
        }
    }

    tasks.register("publishExtension", ExtensionPublishTask::class.java) {
        dependsOn(buildBundle)
        bundle.set(buildBundle.bundlePath)
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
        isEnabled = false
    }
}