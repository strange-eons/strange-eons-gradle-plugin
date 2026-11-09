package org.strangeeons.gradle.plugin;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;

/**
 * The commit time of {@code HEAD}, or no value, with a warning, if the paths
 * have uncommitted changes or git can't tell.
 */
public abstract class CommitTime implements ValueSource<Instant, CommitTime.Parameters> {

    private static final Logger LOG = Logging.getLogger(CommitTime.class);

    public interface Parameters extends ValueSourceParameters {

        /** Folder git runs in. */
        DirectoryProperty getWorkingDir();

        /** Paths whose changes, including untracked files, make the commit time stale. */
        ListProperty<String> getPaths();
    }

    @Inject
    protected abstract ExecOperations getExecOperations();

    @Override
    public Instant obtain() {
        List<String> paths = getParameters().getPaths().get().stream().distinct().toList();
        List<String> status = new ArrayList<>(List.of("status", "--porcelain", "--"));
        status.addAll(paths);
        String changes = git(status);
        if (changes == null) {
            LOG.warn("Could not read git status; stamping catalog IDs with the build time.");
            return null;
        }
        if (!changes.isBlank()) {
            LOG.warn("Uncommitted changes in {}; stamping catalog IDs with the build time.",
                    String.join(", ", paths));
            return null;
        }
        String time = git(List.of("log", "-1", "--format=%cI"));
        if (time == null || time.isBlank()) {
            LOG.warn("Could not read the commit time; stamping catalog IDs with the build time.");
            return null;
        }
        return OffsetDateTime.parse(time.strip()).toInstant();
    }

    // standard output, or null if git fails
    private String git(List<String> args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ExecResult r = getExecOperations().exec(spec -> {
                spec.setWorkingDir(getParameters().getWorkingDir().get().getAsFile());
                spec.executable("git");
                spec.args(args);
                spec.setStandardOutput(out);
                spec.setErrorOutput(new ByteArrayOutputStream());
                spec.setIgnoreExitValue(true);
            });
            return r.getExitValue() == 0 ? out.toString(StandardCharsets.UTF_8) : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
