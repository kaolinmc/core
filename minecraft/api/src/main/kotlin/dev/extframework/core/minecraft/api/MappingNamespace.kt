package dev.extframework.core.minecraft.api

public data class MappingNamespace(
    val organization: String,
    val name: String
) {
    val path: java.nio.file.Path = kotlin.io.path.Path(organization, name)
    val identifier: String = "$organization:$name"

    public companion object {
        public fun parse(identifier: String): MappingNamespace = identifier.split(":").let { (org, name) ->
            MappingNamespace(org, name)
        }
    }
}