package dev.extframework.core.capability.test

import dev.extframework.core.capability.Capability0
import dev.extframework.core.capability.defining
import kotlin.test.Test

class TestBasicCapability {
    val testCapability by TestCapabilities.defining<Capability0<String>>()

    @Test
    fun `Test register and get`() {
        testCapability += Capability0 {
            "Test string"
        }

        check(testCapability.call() == "Test string")
    }
}