package com.kaolinmc.core.entrypoint

public abstract class Entrypoint {
    public abstract fun init()

    public open fun cleanup() {  }
}