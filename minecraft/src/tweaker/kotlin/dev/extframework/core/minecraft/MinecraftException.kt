package dev.extframework.core.minecraft

import dev.extframework.tooling.api.exception.ExceptionType

public enum class MinecraftException : ExceptionType {
    ExtensionDoesNotSupportThisVersion,
    CannotUnloadThis
}