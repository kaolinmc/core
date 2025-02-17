package dev.extframework.core.capability.test;

import dev.extframework.core.capability.Capability;
import dev.extframework.core.capability.Capability0;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static dev.extframework.core.capability.test.TestCapabilities.defineCapability;

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
