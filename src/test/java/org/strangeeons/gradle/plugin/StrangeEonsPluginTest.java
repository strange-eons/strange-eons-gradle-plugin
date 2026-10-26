package org.strangeeons.gradle.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.capabilities.Capability;
import org.gradle.api.tasks.Copy;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StrangeEonsPluginTest {

    @TempDir
    Path tmp;

    private Project project() {
        Project p = ProjectBuilder.builder().withProjectDir(tmp.toFile()).withName("demo").build();
        p.getPluginManager().apply("org.strangeeons.plugin");
        return p;
    }

    private StrangeEonsPluginExtension ext(Project p) {
        return p.getExtensions().getByType(StrangeEonsPluginExtension.class);
    }

    @Test
    void assemblesBundleAndKeepsJar() {
        Project p = project();
        assertTrue(p.getTasks().getByName("jar").getEnabled());
        assertTrue(p.getTasks().getByName("assemble").getTaskDependencies().getDependencies(null)
                .contains(p.getTasks().getByName("sePlugin")));
    }

    @Test
    void offersBundleByCapability() {
        Project p = project();
        p.setGroup("g");
        p.setVersion("1");
        Configuration c = p.getConfigurations().getByName("sePluginElements");
        assertTrue(c.isCanBeConsumed());
        assertFalse(c.isCanBeResolved());
        Capability cap = c.getOutgoing().getCapabilities().iterator().next();
        assertEquals("g:demo-se-plugin:1", cap.getGroup() + ":" + cap.getName() + ":" + cap.getVersion());
        assertEquals("se-bundle", c.getAttributes().getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE).getName());
        assertEquals(p.getTasks().getByName("sePlugin").getOutputs().getFiles().getSingleFile(),
                c.getOutgoing().getArtifacts().getFiles().getSingleFile());
    }

    @Test
    void bundleNamedFromProjectAndRootFile() throws IOException {
        Project p = project();
        ShadowJar bundle = (ShadowJar) p.getTasks().getByName("sePlugin");
        assertEquals("demo.selibrary", bundle.getArchiveFileName().get());

        Path root = tmp.resolve("src/main/resources/eons-plugin");
        Files.createDirectories(root.getParent());
        Files.writeString(root, "name = x\ndemo.DemoPlugin\n");
        assertEquals("demo.seext", bundle.getArchiveFileName().get());

        ext(p).getBundleName().set("Demo");
        ext(p).getBundleType().set("seplugin");
        assertEquals("Demo.seplugin", bundle.getArchiveFileName().get());
    }

    @Test
    void installsIntoUserPluginsFolder() {
        Project p = project();
        ext(p).getUserFolder().set(tmp.resolve("user").toFile());
        Copy install = (Copy) p.getTasks().getByName("installPlugin");
        assertEquals(tmp.resolve("user/plug-ins").toFile(), install.getDestinationDir());
    }

    @Test
    void generatesStampedRootFile() throws IOException {
        Project p = project();
        Path root = tmp.resolve("src/main/resources/eons-plugin");
        Files.createDirectories(root.getParent());
        Files.writeString(root, "#CATALOGUEID{abc:1-2-3-4-5-6-7}\n");
        GenerateRootFile task = (GenerateRootFile) p.getTasks().getByName("generateRootFile");
        task.generate();
        String out = Files.readString(task.getRootFile().get().getAsFile().toPath());
        assertTrue(out.startsWith("#CATALOGUEID{abc:") && !out.contains("1-2-3-4-5-6-7"), out);
    }
}
