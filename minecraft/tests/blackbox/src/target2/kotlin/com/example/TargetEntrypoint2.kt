package com.example

import dev.extframework.core.capability.Capability0
import dev.extframework.core.entrypoint.Entrypoint

class TargetEntrypoint2 : Entrypoint() {
    override fun init() {
        startApp += Capability0 {
        }
    }
}