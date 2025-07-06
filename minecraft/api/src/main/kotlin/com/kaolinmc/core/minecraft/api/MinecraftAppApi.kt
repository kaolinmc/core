package com.kaolinmc.core.minecraft.api

import com.kaolinmc.core.app.api.ApplicationTarget
import java.nio.file.Path

public abstract class MinecraftAppApi : ApplicationTarget {
    public abstract val gameDir: Path
    public abstract val gameJar: Path
    public abstract val classpath: List<Path>
    public abstract val version: String
    public abstract val mainClass: String
}

