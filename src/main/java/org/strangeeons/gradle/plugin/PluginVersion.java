package org.strangeeons.gradle.plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Plug-in version numbers. */
final class PluginVersion {

    private static final Pattern VERSION = Pattern.compile("(\\d+)\\.(\\d)\\.\\d+(-SNAPSHOT)?");

    private PluginVersion() {
    }

    /**
     * Returns a {@code major.minor.patch[-SNAPSHOT]} version as the float SE
     * plug-ins report: {@code 10.0.1} is {@code 10.0}. The minor must be one
     * digit so that 10.10 can't read as 10.1.
     */
    static String seVersion(String version) {
        Matcher m = VERSION.matcher(version);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Version " + version + " is not major.minor.patch[-SNAPSHOT] with a one-digit minor");
        }
        return m.group(1) + "." + m.group(2);
    }
}
