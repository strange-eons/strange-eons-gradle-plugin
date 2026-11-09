package org.strangeeons.gradle.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PluginVersionTest {

    @Test
    void seVersionIsMajorMinor() {
        assertEquals("10.0", PluginVersion.seVersion("10.0.0"));
        assertEquals("10.1", PluginVersion.seVersion("10.1.0-SNAPSHOT"));
        assertEquals("9.9", PluginVersion.seVersion("9.9.12"));
    }

    @Test
    void seVersionRejectsOtherForms() {
        for (String v : new String[] {"10.0", "10.10.0", "10.0.0-rc1", "v10.0.0", "unspecified"}) {
            assertThrows(IllegalArgumentException.class, () -> PluginVersion.seVersion(v), v);
        }
    }
}
