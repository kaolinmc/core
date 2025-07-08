import com.kaolinmc.gradle.common.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("maven-publish")
    id("org.jetbrains.dokka")
    java
    application
    id("com.gradleup.shadow") version "9.0.0-beta11"
    id("com.kaolinmc.common")
    id("io.ktor.plugin") version "3.2.0"
    id("me.champeau.mrjar") version "0.1.1"
}

group = "com.kaolinmc"
version = "1.0.1-BETA"

repositories {
    mavenLocal()
    mavenCentral()
    kaolin()
}

application {
    mainClass = "com.kaolinmc.dev.client.Main"
}

kotlin {
    explicitApi()
}

multiRelease {
    targetVersions(8, 11)
}

dependencies {
    implementation(boot())
    implementation(extLoader())
    implementation(toolingApi())

    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    implementation(archives())
    implementation(objectContainer())
    implementation(artifactResolver())
    implementation(artifactResolverMaven())
    implementation(commonUtil())
    implementation(project(":app:app-api"))
    implementation(project(":minecraft:minecraft-api"))// "com.kaolinmc.core:minecraft-api:1.0-BETA")
    implementation(resourceApi())
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
    implementation(project("client-api"))
    implementation("io.ktor:ktor-server-core:3.2.0")
    implementation("io.ktor:ktor-server-cio:3.2.0")

    "java11Implementation"("com.kaolinmc:boot:${rootProject.extensions.getByType<DependencyManagement>()["boot"]["version"]}:jdk11")
    "java11Implementation"("com.kaolinmc:archives:${rootProject.extensions.getByType<DependencyManagement>()["archives"]["version"]}:jdk11")

    "java11Implementation"(boot())
    "java11Implementation"(objectContainer())


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
    from(tasks.shadowJar)
    isEnabled = false
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
            kaolin(credentials = propertyCredentialProvider, type = RepositoryType.RELEASES)
        }
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}