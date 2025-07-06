package com.kaolinmc.core.capability.test;

import com.kaolinmc.core.capability.Capability;
import com.kaolinmc.core.capability.Capability0;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.kaolinmc.core.capability.test.TestCapabilities.defineCapability;

public class TestBasicJavaCapabilities {
    private final Capability.Reference<Capability0<String>> capability = defineCapability(
            Capability0.class
    );

    @Test
    void capabilities_should_register() {
        TestCapabilities.registerCapability(capability, () -> "test");

        String result = capability.caller().invoke();

        Assertions.assertEquals("test", result);
    }
}
