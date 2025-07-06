package com.kaolinmc.minecraft

import org.gradle.api.Project
import java.nio.file.Paths

public val Project.mavenLocal
    get() = Paths.get(repositories.mavenLocal().url)