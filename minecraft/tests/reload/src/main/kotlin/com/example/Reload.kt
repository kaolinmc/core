package com.example

import dev.extframework.core.entrypoint.Entrypoint

class Reload : Entrypoint() {
    override fun init() {

    }

    override fun cleanup() {
        println("Cleaning up!")
    }
}