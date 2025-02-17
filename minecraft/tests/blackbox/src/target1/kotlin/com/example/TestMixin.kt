package com.example

import dev.extframework.mixin.api.InjectCode
import dev.extframework.mixin.api.Mixin
import dev.extframework.mixin.api.MixinFlow

@Mixin(App::class)
class TestMixin {
    @InjectCode("main")
    fun injector(flow: MixinFlow) {
        println("This is a mixin")
    }
}