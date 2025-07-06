pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.kaolinmc.com/releases")
        }
        gradlePluginPortal()
        mavenLocal()
    }
}

rootProject.name = "buildSrc"

