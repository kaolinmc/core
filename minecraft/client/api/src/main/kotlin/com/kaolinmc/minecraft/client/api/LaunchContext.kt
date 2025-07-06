package com.kaolinmc.minecraft.client.api

import java.nio.file.Path

public data class LaunchContext(
    val targetExtension: String,
    // The local repository path
    val repository: String,
    val namespace: String,
    val extensionDirectory: Path,

    val version: String,
    val mainClass: String,
    val classpath: List<Path>,
    val gameJar: Path,

    val gameArguments: List<String>
)