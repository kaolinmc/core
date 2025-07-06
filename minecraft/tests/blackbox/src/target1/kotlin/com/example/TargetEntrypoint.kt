package com.example

import com.kaolinmc.core.capability.Capability0
import com.kaolinmc.core.entrypoint.Entrypoint
import net.minecraft.server.Bootstrap

class TargetEntrypoint : Entrypoint() {
    override fun init() {
        startApp += Capability0 {
            println("Capability")
            Bootstrap.realStdoutPrintln("SOMETHING ELSE")
        }
    }
}