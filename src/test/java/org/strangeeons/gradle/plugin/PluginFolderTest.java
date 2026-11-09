package org.strangeeons.gradle.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PluginFolderTest {

    @TempDir
    Path dir;

    private void write(String rel, String text) throws IOException {
        Path p = dir.resolve(rel);
        Files.createDirectories(p.getParent());
        Files.writeString(p, text);
    }

    @Test
    void membersExcludeWhatSeExcludes() {
        for (String name : new String[] {"seproject", ".hidden", "x.ts.js", "Thumbs.db", "THUMBS.DB", "_vti_cnf",
                ".gitignore", ".svn"}) {
            assertFalse(PluginFolder.isMember(dir.resolve(name).toFile()), name);
        }
        for (String name : new String[] {"eons-plugin", "README.md", "x.js", "resources", "git"}) {
            assertTrue(PluginFolder.isMember(dir.resolve(name).toFile()), name);
        }
    }

    @Test
    void guessesExtensionFromScript() throws IOException {
        write("eons-plugin", "name = x\nres://game/Game.js\n");
        write("resources/game/Game.js", "useLibrary('extension');\n");
        assertEquals("seext", PluginFolder.guessType(dir));
        write("resources/game/Game.js", "function getPluginType() { return arkham.plugins.Plugin.EXTENSION; }\n");
        assertEquals("seext", PluginFolder.guessType(dir));
    }

    @Test
    void guessesPluginFromOtherScripts() throws IOException {
        write("eons-plugin", "script:res:///Tool.js\n");
        write("Tool.js", "function run() {}\n");
        assertEquals("seplugin", PluginFolder.guessType(dir));
        write("eons-plugin", "Missing.js\n");
        assertEquals("seplugin", PluginFolder.guessType(dir));
    }

    @Test
    void guessesOtherRootFilesFromTheirLines() throws IOException {
        assertEquals("selibrary", PluginFolder.guessType(dir));
        write("eons-plugin", "name = x\n");
        assertEquals("selibrary", PluginFolder.guessType(dir));
        write("eons-plugin", "game.DarkTheme\n");
        assertEquals("setheme", PluginFolder.guessType(dir));
    }
}
