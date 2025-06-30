package com.example

import dev.extframework.core.capability.Capability0
import dev.extframework.core.entrypoint.Entrypoint
import net.minecraft.resources.ResourceLocation

class TargetEntrypoint : Entrypoint() {
    override fun init() {
        startApp += Capability0 {
            println("Capability")
//            println(ResourceLocation("extframework", "test"))
        }
    }
}