package org.strangeeons.gradle.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RootFileTest {

    private static final Instant TIME = Instant.parse("2026-01-05T03:04:05.006Z");

    @Test
    void dateStringMatchesCatalogId() {
        assertEquals("2026-0-5-3-4-5-6", RootFile.dateString(TIME));
        assertEquals("2026-11-31-23-59-59-999", RootFile.dateString(Instant.parse("2026-12-31T23:59:59.999Z")));
    }

    @Test
    void stampReplacesDateAndKeepsUuid() {
        String in = "#CATALOGUEID{97b691b4-48e8-4649-aa10-b2969b378828:2026-5-3-0-0-0-0}\nname = x\n";
        assertEquals("#CATALOGUEID{97b691b4-48e8-4649-aa10-b2969b378828:2026-0-5-3-4-5-6}\nname = x\n",
                RootFile.stamp(in, TIME));
    }

    @Test
    void stampAcceptsCatalogIdSpellings() {
        assertEquals("catalogid{AB-12:2026-0-5-3-4-5-6}", RootFile.stamp("catalogid{AB-12:1-2-3-4-5-6-7}", TIME));
        assertEquals("CATALOGID{ab:2026-0-5-3-4-5-6}", RootFile.stamp("CATALOGID{ab:1-2-3-4-5-6-7}", TIME));
    }

    @Test
    void stampLeavesTextWithoutIdUnchanged() {
        assertEquals("name = x\nid = CATALOGUEID{nothex:1-2-3}\n",
                RootFile.stamp("name = x\nid = CATALOGUEID{nothex:1-2-3}\n", TIME));
    }

    @Test
    void guessesLibraryWithoutPluginLine() {
        assertEquals("selibrary", RootFile.guessType(List.of()));
        assertEquals("selibrary", RootFile.guessType(List.of("#CATALOGUEID{x:1-2-3-4-5-6-7}", "name = Lib")));
    }

    @Test
    void guessesExtensionOrThemeFromClass() {
        assertEquals("seext", RootFile.guessType(List.of("name = x", "mygame.plugin.MyGamePlugin")));
        assertEquals("setheme", RootFile.guessType(List.of("class:mygame.DarkTheme")));
    }

    @Test
    void firstPluginIdentifierSkipsKeysCommentsAndContinuations() {
        assertNull(RootFile.firstPluginIdentifier(List.of("# c", "! c", "a = b \\", "  res://not/this.js", "")));
        assertEquals("res://game/Game.js", RootFile.firstPluginIdentifier(List.of("a: b", "res://game/Game.js")));
        assertEquals("script:x.js", RootFile.firstPluginIdentifier(List.of("script:x.js")));
        assertEquals("class:a.B", RootFile.firstPluginIdentifier(List.of("class:a.B")));
    }
}
