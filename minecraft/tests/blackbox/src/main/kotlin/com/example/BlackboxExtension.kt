package com.example

import com.kaolinmc.core.capability.Capability
import com.kaolinmc.core.capability.Capability0
import com.kaolinmc.core.capability.defining
import com.kaolinmc.core.entrypoint.Entrypoint
import com.kaolinmc.core.minecraft.api.TargetCapabilities

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