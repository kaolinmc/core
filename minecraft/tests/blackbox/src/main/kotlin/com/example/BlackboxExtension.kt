package com.example

import dev.extframework.core.capability.Capability
import dev.extframework.core.capability.Capability0
import dev.extframework.core.capability.defining
import dev.extframework.core.entrypoint.Entrypoint
import dev.extframework.core.minecraft.api.TargetCapabilities

public val startApp: Capability.Reference<Capability0<Unit>> by TargetCapabilities.defining()

public class BlackboxExtension : Entrypoint() {
    override fun init() {
        println("Init v2")
        startApp.call()
    }

    override fun cleanup() {
        println("Cleaning up capabilities")
        startApp.cleanup()
        println("Cleaned")
        System.setProperty("cleaned", "true")
    }
}