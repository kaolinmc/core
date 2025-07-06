package com.example

import com.kaolinmc.mixin.api.InjectCode
import com.kaolinmc.mixin.api.Mixin
import com.kaolinmc.mixin.api.MixinFlow
import targetapp.Main

@Mixin(Main::class)
class ReloadableMixin {
    @InjectCode
    fun main(
        flow: MixinFlow
    ) : MixinFlow.Result<Int> {
        println("Here from mixin!")
        return flow.yield(6)
    }
}