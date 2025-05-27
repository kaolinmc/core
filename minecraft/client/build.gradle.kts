import dev.extframework.gradle.common.*
import dev.extframework.gradle.common.dm.artifactResolver
import dev.extframework.gradle.common.dm.jobs
import dev.extframework.gradle.common.dm.resourceApi
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("maven-publish")
    id("org.jetbrains.dokka")
    java
    application
    id("com.gradleup.shadow") version "9.0.0-beta11"
    id("dev.extframework.common")

    id("me.champeau.mrjar") version "0.1.1"
}

group = "dev.extframework"
version = "1.0-BETA"

repositories {
    mavenLocal()
    mavenCentral()
    extFramework()
}

application {
    mainClass = "dev.extframework.dev.client.Main"
}

kotlin {
    explicitApi()
}

multiRelease {
    targetVersions(8, 11)
}

dependencies {
    boot(version = "3.6.2-SNAPSHOT")
    extLoader(version = "2.1.17-SNAPSHOT")
    toolingApi(version = "1.0.8-SNAPSHOT")

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    jobs()
    archives()
    objectContainer()
    artifactResolver(maven = true)
    commonUtil()
    implementation(project(":app:app-api"))
    implementation(project(":minecraft:minecraft-api"))// "dev.extframework.core:minecraft-api:1.0-BETA")
    resourceApi(version = "1.2.1-SNAPSHOT")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.3")
    implementation(project("client-api"))

    implementation("dev.extframework:boot:$BOOT_VERSION:jdk11")
    implementation("dev.extframework:archives:$ARCHIVES_VERSION:jdk11")


    "java11Implementation"("dev.extframework:boot:$BOOT_VERSION:jdk11")
    boot(configurationName = "java11Implementation")
    objectContainer(configurationName = "java11Implementation")


    testImplementation(kotlin("test"))
}

abstract class ListAllDependencies : DefaultTask() {
    init {
        // Define the output file within the build directory
        val outputFile = project.buildDir.resolve("resources/main/dependencies.txt")
        outputs.file(outputFile)
    }

    @TaskAction
    fun listDependencies() {
        val outputFile = project.buildDir.resolve("resources/main/dependencies.txt")
        // Ensure the directory for the output file exists
        outputFile.parentFile.mkdirs()
        // Clear or create the output file
        outputFile.writeText("")

        val set = HashSet<String>()

        // Process each configuration that can be resolved
        project.configurations.filter { it.isCanBeResolved }.forEach { configuration ->
            println("Processing configuration: ${configuration.name}")
            try {
                configuration.resolvedConfiguration.firstLevelModuleDependencies.forEach { dependency ->
                    collectDependencies(dependency, set)
                }
            } catch (e: Exception) {
                println("Skipping configuration '${configuration.name}' due to resolution errors.")
            }
        }

        set.forEach {
            outputFile.appendText(it)
        }
    }

    private fun collectDependencies(dependency: ResolvedDependency, set: MutableSet<String>) {
        set.add("${dependency.moduleGroup}:${dependency.moduleName}:${dependency.moduleVersion}\n")
        dependency.children.forEach { childDependency ->
            collectDependencies(childDependency, set)
        }
    }
}

tasks.named<Test>("java11Test") {
    description = "Runs tests in the java11Test source set"
    group = "verification"

    testClassesDirs = sourceSets["java11Test"].output.classesDirs
    classpath = sourceSets["java11Test"].runtimeClasspath

    // Use JUnit 5 if applicable
    useJUnitPlatform()
}

tasks.named<KotlinCompile>("compileJava11Kotlin") {
    kotlinJavaToolchain.toolchain.use(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(11))
    })
    kotlinOptions.jvmTarget = "11"
    kotlinOptions.freeCompilerArgs += "-Xexplicit-api=strict"
}

tasks.named<JavaCompile>("compileJava11Java") {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

tasks.named<JavaCompile>("compileJava11TestJava") {
    sourceCompatibility = "11"
    targetCompatibility = "11"
}

// Register the custom task in the project
val listAllDependencies by tasks.registering(ListAllDependencies::class)

tasks.compileKotlin {
    dependsOn(listAllDependencies)
}

tasks.shadowJar {
    from(tasks.named("listAllDependencies"))
    from(sourceSets["java11"].output) {
        into("META-INF/versions/11")
    }
    archiveClassifier = ""
    manifest {
        attributes("Multi-Release" to true)
    }
}

tasks.jar {
    enabled = false
    dependsOn(tasks.shadowJar)
}

common {
    defaultJavaSettings()

    publishing {
        publication {
            withSources()
            withDokka()
            artifact(tasks.shadowJar)

            artifactId = "client"
        }
        repositories {
            extFramework(credentials = propertyCredentialProvider, type = RepositoryType.RELEASES)
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}