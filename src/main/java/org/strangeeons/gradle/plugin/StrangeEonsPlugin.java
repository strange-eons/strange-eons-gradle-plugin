package org.strangeeons.gradle.plugin;

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.attributes.Bundling;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * Packages a JVM project as a Strange Eons plug-in bundle alongside the
 * plain JAR:
 *
 * <ul>
 * <li>{@code generateRootFile}: writes {@code eons-plugin} with a fresh catalog ID timestamp</li>
 * <li>{@code sePlugin}: builds the bundle from the main classes and resources plus the
 * runtime classpath, merging {@code META-INF/services} (part of {@code assemble})</li>
 * <li>{@code installPlugin}: copies the bundle into the SE user plug-ins folder</li>
 * </ul>
 *
 * Other projects get the plain JAR as usual. The bundle is offered to them as
 * the unpublished {@code sePluginElements} variant, selected by requiring the
 * capability {@code <group>:<name>-se-plugin}.
 */
public abstract class StrangeEonsPlugin implements Plugin<Project> {

    public static final String GROUP = "strange eons";
    public static final String EXTENSION_NAME = "strangeEonsPlugin";
    public static final String GENERATE_ROOT_FILE_TASK_NAME = "generateRootFile";
    public static final String BUNDLE_TASK_NAME = "sePlugin";
    public static final String INSTALL_TASK_NAME = "installPlugin";
    public static final String ROOT_FILE_NAME = "eons-plugin";
    public static final String ELEMENTS_CONFIGURATION_NAME = "sePluginElements";
    public static final String CAPABILITY_SUFFIX = "-se-plugin";
    public static final String LIBRARY_ELEMENTS = "se-bundle";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(JavaPlugin.class);
        StrangeEonsPluginExtension ext = project.getExtensions().create(EXTENSION_NAME, StrangeEonsPluginExtension.class);

        ext.getBundleName().convention(project.getName());
        ext.getRootFile().convention(project.getLayout().getProjectDirectory().file("src/main/resources/" + ROOT_FILE_NAME));
        ext.getBundleType().convention(ext.getRootFile().map(f -> guessType(f.getAsFile())));
        ext.getUserFolder().convention(project.getLayout().dir(project.getProviders().provider(() -> UserFolder.resolve(
                System.getenv(), System.getProperty("os.name"), System.getProperty("user.home")))));

        TaskProvider<GenerateRootFile> generate = project.getTasks().register(GENERATE_ROOT_FILE_TASK_NAME,
                GenerateRootFile.class, t -> {
                    t.setGroup(GROUP);
                    t.setDescription("Writes the bundle's eons-plugin root file with a fresh catalog ID timestamp.");
                    t.getSource().set(ext.getRootFile());
                    t.getRootFile().set(project.getLayout().getBuildDirectory().file("generated/strange-eons/" + ROOT_FILE_NAME));
                });

        SourceSet main = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets()
                .getByName(SourceSet.MAIN_SOURCE_SET_NAME);

        TaskProvider<ShadowJar> bundle = project.getTasks().register(BUNDLE_TASK_NAME, ShadowJar.class, t -> {
            t.setGroup(GROUP);
            t.setDescription("Builds the Strange Eons plug-in bundle.");
            t.from(main.getOutput(), spec -> spec.exclude(ROOT_FILE_NAME));
            t.from(generate);
            t.getConfigurations().add(project.getConfigurations().getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME));
            // Same exclusions as Shadow's own shadowJar task.
            t.exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA",
                    "META-INF/versions/**/module-info.class", "module-info.class");
            // Shadow drops duplicates before transforming unless they are included.
            t.filesMatching("META-INF/services/**", f -> f.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE));
            t.mergeServiceFiles();
            t.getArchiveBaseName().set(ext.getBundleName());
            t.getArchiveVersion().set("");
            t.getArchiveClassifier().set("");
            t.getArchiveExtension().set(ext.getBundleType());
        });

        Provider<Directory> pluginsFolder = ext.getUserFolder().dir("plug-ins");
        project.getTasks().register(INSTALL_TASK_NAME, Copy.class, t -> {
            t.setGroup(GROUP);
            t.setDescription("Copies the bundle into the Strange Eons user plug-ins folder.");
            t.from(bundle);
            t.into(pluginsFolder);
            t.onlyIf("plug-ins folder exists", x -> pluginsFolder.get().getAsFile().isDirectory());
        });

        ObjectFactory objects = project.getObjects();
        project.getConfigurations().consumable(ELEMENTS_CONFIGURATION_NAME, c -> {
            c.setDescription("The Strange Eons plug-in bundle.");
            c.attributes(a -> {
                a.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.class, Usage.JAVA_RUNTIME));
                a.attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.class, Category.LIBRARY));
                a.attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.class, Bundling.SHADOWED));
                a.attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
                        objects.named(LibraryElements.class, LIBRARY_ELEMENTS));
            });
            c.getOutgoing().capability(project.provider(() ->
                    project.getGroup() + ":" + project.getName() + CAPABILITY_SUFFIX + ":" + project.getVersion()));
            c.getOutgoing().artifact(bundle);
        });

        project.getTasks().named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME, t -> t.dependsOn(bundle));
    }

    private static String guessType(File rootFile) {
        if (!rootFile.isFile()) {
            return RootFile.guessType(List.of());
        }
        try {
            return RootFile.guessType(Files.readAllLines(rootFile.toPath(), StandardCharsets.ISO_8859_1));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
