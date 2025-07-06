package com.example

import com.kaolinmc.core.entrypoint.Entrypoint

class Reload : Entrypoint() {
    override fun init() {

    }

    override fun cleanup() {
        println("Cleaning up!")
    }
}