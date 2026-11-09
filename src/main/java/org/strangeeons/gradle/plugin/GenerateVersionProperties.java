package org.strangeeons.gradle.plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/**
 * Writes the project version, and the float form SE plug-ins report, as a
 * properties file at {@link #getFilePath()} in the output folder. Writes nothing
 * when the path is unset.
 */
@DisableCachingByDefault(because = "writes one small file, faster than a cache lookup")
public abstract class GenerateVersionProperties extends DefaultTask {

    /** Path of the file within the bundle, e.g. {@code resources/mygame/version.properties}. */
    @Input
    @Optional
    public abstract Property<String> getFilePath();

    @Input
    public abstract Property<String> getVersion();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @TaskAction
    void generate() throws IOException {
        Path dir = getOutputDir().get().getAsFile().toPath();
        getFileSystemOperations().delete(spec -> spec.delete(dir.toFile()));
        Files.createDirectories(dir);
        if (!getFilePath().isPresent()) {
            return;
        }
        Path out = dir.resolve(getFilePath().get()).normalize();
        if (!out.startsWith(dir) || out.equals(dir)) {
            throw new IllegalArgumentException("versionProperties " + getFilePath().get() + " is not a path in the bundle");
        }
        String version = getVersion().get();
        Files.createDirectories(out.getParent());
        Files.writeString(out, "version=" + version + "\nse-version=" + PluginVersion.seVersion(version) + "\n",
                StandardCharsets.ISO_8859_1);
    }
}
