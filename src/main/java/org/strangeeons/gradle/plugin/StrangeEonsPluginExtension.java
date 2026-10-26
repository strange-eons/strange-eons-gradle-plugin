package org.strangeeons.gradle.plugin;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

/**
 * Configuration for packaging a project as a Strange Eons plug-in bundle.
 * Registered as {@code strangeEonsPlugin}.
 *
 * <pre>
 * strangeEonsPlugin {
 *     bundleName = "MyGame"          // MyGame.seext
 *     bundleType = "seext"           // guessed from the root file if unset
 *     rootFile = file("src/main/resources/eons-plugin")
 * }
 * </pre>
 */
public abstract class StrangeEonsPluginExtension {

    /** Bundle file name without extension. Defaults to the project name. */
    public abstract Property<String> getBundleName();

    /**
     * Bundle extension without the dot: {@code selibrary}, {@code seext},
     * {@code seplugin} or {@code setheme}. Defaults to the type SE's
     * "Make Bundle" command would guess from {@link #getRootFile()}.
     */
    public abstract Property<String> getBundleType();

    /**
     * The {@code eons-plugin} root file. Its catalog ID is stamped with the
     * build time when packaged. Defaults to
     * {@code src/main/resources/eons-plugin}.
     */
    public abstract RegularFileProperty getRootFile();

    /**
     * The Strange Eons user folder {@code installPlugin} copies into (its
     * {@code plug-ins} subfolder). Defaults to {@code STRANGE_EONS_USER_DIR},
     * else the platform default SE uses.
     */
    public abstract DirectoryProperty getUserFolder();
}
