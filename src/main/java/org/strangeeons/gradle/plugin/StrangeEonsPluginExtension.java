package org.strangeeons.gradle.plugin;

import java.time.Instant;
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
 *     pluginFolder = file("MyGame")   // a script plug-in's project folder
 *     versionProperties = "resources/mygame/version.properties"
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
     * The {@code eons-plugin} root file. Its catalog ID is stamped with
     * {@link #getCatalogTimestamp()} when packaged. Defaults to the {@link #getPluginFolder()}'s
     * root file if set, else {@code src/main/resources/eons-plugin}.
     */
    public abstract RegularFileProperty getRootFile();

    /**
     * The time stamped into the root file's catalog IDs. SE offers a bundle as
     * an update when this is newer than the installed bundle's. Defaults to
     * the build time, which makes every build new; a fixed time, such as the
     * commit time, lets an unchanged project's bundle stay up to date.
     */
    public abstract Property<Instant> getCatalogTimestamp();

    /**
     * Whether {@link #getCatalogTimestamp()} defaults to the commit time of
     * {@code HEAD}. The build time is used instead, with a warning, when the
     * project or plug-in folder has uncommitted changes or git can't tell.
     * Defaults to {@code false}.
     */
    public abstract Property<Boolean> getCatalogTimestampFromCommit();

    /**
     * An SE plug-in project folder, holding {@code eons-plugin} and
     * {@code resources/}, for plug-ins made of scripts and resources. Its
     * members are packed as SE's "Make Bundle" command packs them, alongside
     * any compiled classes and resources. Optional.
     */
    public abstract DirectoryProperty getPluginFolder();

    /**
     * Path in the bundle of a generated properties file holding
     * {@code version}, the project version, and {@code se-version}, the float
     * form SE plug-ins report ({@code 10.0.1} is {@code 10.0}). Optional.
     */
    public abstract Property<String> getVersionProperties();

    /**
     * The Strange Eons user folder {@code installPlugin} copies into (its
     * {@code plug-ins} subfolder). Defaults to {@code STRANGE_EONS_USER_DIR},
     * else the platform default SE uses.
     */
    public abstract DirectoryProperty getUserFolder();
}
