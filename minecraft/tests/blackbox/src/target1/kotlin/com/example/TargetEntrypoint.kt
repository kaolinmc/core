package com.example

import dev.extframework.core.capability.Capability0
import dev.extframework.core.entrypoint.Entrypoint

class TargetEntrypoint : Entrypoint() {
    override fun init() {
        startApp += Capability0 {
            App().main()
        }
    }
}