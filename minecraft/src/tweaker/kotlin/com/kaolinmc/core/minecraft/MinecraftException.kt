package com.kaolinmc.core.minecraft

import com.kaolinmc.tooling.api.exception.ExceptionType

public enum class MinecraftException : ExceptionType {
    ExtensionDoesNotSupportThisVersion,
    CannotUnloadThis
}