import com.kaolinmc.gradle.common.boot
import com.kaolinmc.gradle.common.extLoader
import com.kaolinmc.gradle.common.objectContainer
import com.kaolinmc.gradle.common.toolingApi

group = "com.kaolinmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
}

dependencies {
    implementation(boot())
    implementation(toolingApi())

    implementation(project(":instrument"))
    implementation(project(":app"))
    implementation(project(":app:app-api"))
    implementation(extLoader())
//    implementation(artifactResolver())
    implementation(objectContainer())
    implementation(project(":minecraft:minecraft-api"))
    implementation(project(":minecraft:client:client-api"))
//    implementation(resourceApi())

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "21"
    }
}

kotlin {
    jvmToolchain(21)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val listDependencies by tasks.registering(ListAllDependencies::class) {
    output.set(project.layout.buildDirectory.file("resources/test/dependencies.txt"))
}

tasks.test {
    useJUnitPlatform()

    dependsOn(listDependencies)

//    dependsOn(project(":main").tasks.named("publishToMavenLocal"))
//    dependsOn(project(":app").tasks.named("publishToMavenLocal"))
//    dependsOn(project(":minecraft:blackbox").tasks.named("publishToMavenLocal"))
//    dependsOn(project(":minecraft").tasks.named("publishToMavenLocal"))
//    dependsOn(project(":minecraft:api").tasks.named("publishToMavenLocal"))
//    dependsOn(project(":app:app-api").tasks.named("publishToMavenLocal"))
//    dependsOn(project(":entrypoint").tasks.named("publishToMavenLocal"))
}