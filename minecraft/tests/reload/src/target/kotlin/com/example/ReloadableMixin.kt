package com.example

import dev.extframework.mixin.api.InjectCode
import dev.extframework.mixin.api.Mixin
import dev.extframework.mixin.api.MixinFlow
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