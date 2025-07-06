package com.kaolinmc.core.app.api

import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenDescriptor
import com.kaolinmc.boot.archive.ClassLoadedArchiveNode
import com.kaolinmc.tooling.api.environment.ExtensionEnvironment
import java.nio.file.Path

public typealias ApplicationDescriptor = SimpleMavenDescriptor

public interface ApplicationTarget : ExtensionEnvironment.Attribute {
    override val key: ExtensionEnvironment.Attribute.Key<*>
        get() = ApplicationTarget

    public val node: ClassLoadedArchiveNode<ApplicationDescriptor>
    public val path: Path

    public companion object : ExtensionEnvironment.Attribute.Key<ApplicationTarget>
}