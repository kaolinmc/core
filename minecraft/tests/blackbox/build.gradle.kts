import dev.extframework.gradle.common.mixin

group = "com.example"
version = "1"

sourceSets {
    create("target1")
}

dependencies {
    implementation(project(":minecraft:minecraft-api"))
    "target1Implementation"(project(":minecraft:minecraft-api"))

    "target1Implementation"(project(":minecraft:blackbox-app"))

    implementation(project(":capability"))
    "target1Implementation"(project(":capability"))


    mixin(configurationName = "target1Implementation")
    implementation(project(":entrypoint"))
    "target1Implementation"(project(":entrypoint"))
    "target1Implementation"(sourceSets.main.get().output)
}

val generateTargetPrm by tasks.registering(GeneratePrm::class) {
    sourceSetName.set("target1")
    prm.set(
        PartitionRuntimeModel(
            "minecraft", "target1",
            options = mutableMapOf(
                "versions" to "1",
                "entrypoint" to "com.example.TargetEntrypoint"
            )
        )
    )
    includeMavenLocal.set(true)
    ignoredModules.addAll(
        "dev.extframework.core:blackbox-app"
    )
}

val generateMainPrm by tasks.registering(GeneratePrm::class) {
    sourceSetName.set("main")
    prm.set(
        PartitionRuntimeModel(
            "main", "main",
            options = mutableMapOf(
                "extension-class" to "com.example.BlackboxExtension"
            )
        )
    )
    includeMavenLocal.set(true)
    ignoredModules.addAll(
    )
}

val generateErm by tasks.registering(GenerateErm::class) {
    partitions {
        add(generateMainPrm.get())
        add(generateTargetPrm.get())
    }
    includeMavenLocal.set(true)
    parents {
        add(project(":minecraft"))
    }
}

val target1Jar by tasks.registering(Jar::class) {
    from(sourceSets.named("target1").get().output)
    archiveClassifier.set("target1")
}

extensions.getByType(PublishingExtension::class).apply {
    publications {
        create("maven", MavenPublication::class.java) {
            artifact(generateErm).classifier = "erm"
            artifact(tasks.named("jar")).classifier = "main"
            artifact(target1Jar).classifier = "target1"
        }
    }
}