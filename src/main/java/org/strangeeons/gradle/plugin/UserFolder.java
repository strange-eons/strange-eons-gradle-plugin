package org.strangeeons.gradle.plugin;

import java.io.File;
import java.util.Map;

/** Locates the Strange Eons user folder the way SE does. */
final class UserFolder {

    static final String OVERRIDE_VARIABLE = "STRANGE_EONS_USER_DIR";

    private UserFolder() {
    }

    static File resolve(Map<String, String> env, String osName, String userHome) {
        String override = env.get(OVERRIDE_VARIABLE);
        if (override != null && !override.isEmpty()) {
            return new File(override);
        }
        if (osName.toLowerCase().startsWith("windows")) {
            String appData = env.get("APPDATA");
            return new File(appData == null || appData.isEmpty() ? userHome : appData, "StrangeEons3");
        }
        return new File(userHome, ".StrangeEons3");
    }
}
