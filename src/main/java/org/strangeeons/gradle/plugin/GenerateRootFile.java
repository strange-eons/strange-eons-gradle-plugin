package org.strangeeons.gradle.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/**
 * Writes the bundle's {@code eons-plugin} root file with each catalog ID
 * stamped with the build time, so SE sees every build as an update.
 */
@DisableCachingByDefault(because = "output embeds the build time")
public abstract class GenerateRootFile extends DefaultTask {

    public GenerateRootFile() {
        getOutputs().upToDateWhen(t -> false);
    }

    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getSource();

    @OutputFile
    public abstract RegularFileProperty getRootFile();

    @TaskAction
    void generate() throws IOException {
        // ISO-8859-1 round-trips every byte, matching how SE reads the file.
        String text = Files.readString(getSource().get().getAsFile().toPath(), StandardCharsets.ISO_8859_1);
        Path out = getRootFile().get().getAsFile().toPath();
        Files.createDirectories(out.getParent());
        Files.writeString(out, RootFile.stamp(text, Instant.now()), StandardCharsets.ISO_8859_1);
    }
}
