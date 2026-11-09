package org.strangeeons.gradle.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

/** An SE plug-in project folder, packed the way SE's "Make Bundle" command packs it. */
final class PluginFolder {

    /** Default value of SE's {@code exclude-from-projects} setting. */
    private static final List<Pattern> EXCLUDES = List.of("Thumbs.db", "_vti_cnf", ".hg*", ".svn*", ".git*").stream()
            .map(PluginFolder::glob).toList();

    private static final Pattern RETURNS_EXTENSION = Pattern.compile("return\\s+(\\p{Alnum}+\\.)*EXTENSION");
    private static final Pattern USES_EXTENSION = Pattern.compile("use[lL]ibrary\\s*\\(\\s*['\"]extension['\"]");

    private PluginFolder() {
    }

    /** Whether SE packs this file or folder; mirrors {@code Member.FILE_FILTER}. */
    static boolean isMember(File file) {
        String name = file.getName();
        if (file.isHidden() || name.isEmpty() || name.charAt(0) == '.' || name.equals("seproject")
                || name.endsWith(".ts.js")) {
            return false;
        }
        return EXCLUDES.stream().noneMatch(x -> x.matcher(name).matches());
    }

    /**
     * Guesses the bundle type the way {@code MakeBundle.guessPluginType}
     * does: a script plug-in is an extension if its script uses the
     * {@code extension} library or returns {@code EXTENSION}, else a plug-in;
     * other root files are guessed by {@link RootFile#guessType}.
     */
    static String guessType(Path folder) throws IOException {
        Path root = folder.resolve(StrangeEonsPlugin.ROOT_FILE_NAME);
        if (!Files.isRegularFile(root)) {
            return RootFile.guessType(List.of());
        }
        List<String> lines = Files.readAllLines(root, StandardCharsets.ISO_8859_1);
        String id = RootFile.firstPluginIdentifier(lines);
        if (id == null) {
            return RootFile.guessType(lines);
        }
        if (id.endsWith(".js") && !id.startsWith("class:") && !id.startsWith("script:")) {
            id = "script:" + id;
        }
        if (id.startsWith("res:")) {
            id = "script:" + id;
        }
        if (!id.startsWith("script:")) {
            return RootFile.guessType(lines);
        }
        String path = id.substring("script:".length());
        if (path.startsWith("res://")) {
            path = path.substring("res://".length());
            path = path.startsWith("/") ? path.substring(1) : "resources/" + path;
        }
        Path script = folder.resolve(path);
        if (Files.isRegularFile(script)) {
            String text = Files.readString(script, StandardCharsets.UTF_8);
            if (RETURNS_EXTENSION.matcher(text).find() || USES_EXTENSION.matcher(text).find()) {
                return "seext";
            }
        }
        return "seplugin";
    }

    private static Pattern glob(String g) {
        StringBuilder b = new StringBuilder();
        for (char c : g.toCharArray()) {
            switch (c) {
                case '*' -> b.append(".*");
                case '?' -> b.append('.');
                default -> b.append(Pattern.quote(String.valueOf(c)));
            }
        }
        return Pattern.compile(b.toString(), Pattern.CASE_INSENSITIVE);
    }
}
