package com.kaolinmc.core.minecraft.api

import java.io.Serializable

public data class MappingNamespace(
    val organization: String,
    val name: String
) : Serializable {
    @Transient
    val path: java.nio.file.Path = kotlin.io.path.Path(organization, name)
    @Transient
    val identifier: String = "$organization:$name"

    public companion object {
        public fun parse(identifier: String): MappingNamespace = identifier.split(":").let { (org, name) ->
            MappingNamespace(org, name)
        }
    }
}