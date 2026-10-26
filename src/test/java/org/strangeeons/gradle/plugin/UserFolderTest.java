package org.strangeeons.gradle.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.Map;
import org.junit.jupiter.api.Test;

class UserFolderTest {

    @Test
    void overrideWins() {
        assertEquals(new File("/se"), UserFolder.resolve(Map.of("STRANGE_EONS_USER_DIR", "/se", "APPDATA", "/a"),
                "Windows 11", "/home"));
    }

    @Test
    void emptyOverrideIsIgnored() {
        assertEquals(new File("/home", ".StrangeEons3"),
                UserFolder.resolve(Map.of("STRANGE_EONS_USER_DIR", ""), "Mac OS X", "/home"));
    }

    @Test
    void windowsUsesAppData() {
        assertEquals(new File("/a", "StrangeEons3"), UserFolder.resolve(Map.of("APPDATA", "/a"), "Windows 11", "/home"));
    }

    @Test
    void windowsFallsBackToHome() {
        assertEquals(new File("/home", "StrangeEons3"), UserFolder.resolve(Map.of(), "Windows 11", "/home"));
        assertEquals(new File("/home", "StrangeEons3"), UserFolder.resolve(Map.of("APPDATA", ""), "Windows 11", "/home"));
    }

    @Test
    void othersUseDotFolder() {
        assertEquals(new File("/home", ".StrangeEons3"), UserFolder.resolve(Map.of(), "Linux", "/home"));
    }
}
