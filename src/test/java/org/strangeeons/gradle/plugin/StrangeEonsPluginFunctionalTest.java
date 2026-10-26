package org.strangeeons.gradle.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StrangeEonsPluginFunctionalTest {

    private static final String ID = "97b691b4-48e8-4649-aa10-b2969b378828";

    @TempDir
    Path dir;

    private void write(String rel, String text) throws IOException {
        Path p = dir.resolve(rel);
        Files.createDirectories(p.getParent());
        Files.writeString(p, text);
    }

    private void writeDependencyJar() throws IOException {
        Files.createDirectories(dir.resolve("libs"));
        try (OutputStream out = Files.newOutputStream(dir.resolve("libs/dep.jar"));
                ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("META-INF/services/demo.Service"));
            zip.write("dep.Impl\n".getBytes(StandardCharsets.UTF_8));
            zip.putNextEntry(new ZipEntry("dep/Lib.txt"));
            zip.write("x".getBytes(StandardCharsets.UTF_8));
            zip.putNextEntry(new ZipEntry("module-info.class"));
            zip.closeEntry();
        }
    }

    private void writeProject(String extension) throws IOException {
        write("settings.gradle.kts", "rootProject.name = \"demo\"\n");
        write("build.gradle.kts", """
                plugins { id("org.strangeeons.plugin") }
                dependencies { implementation(files("libs/dep.jar")) }
                strangeEonsPlugin {
                    userFolder = layout.projectDirectory.dir("user")
                %s}
                """.formatted(extension));
        write("src/main/java/demo/Demo.java", "package demo; public class Demo {}\n");
        write("src/main/resources/META-INF/services/demo.Service", "demo.Impl\n");
        write("src/main/resources/eons-plugin", "#CATALOGUEID{" + ID + ":2026-5-3-0-0-0-0}\nname = Demo\n");
        writeDependencyJar();
    }

    private BuildResult run(String... args) {
        return GradleRunner.create().withProjectDir(dir.toFile()).withPluginClasspath().withArguments(args).build();
    }

    private static String text(JarFile jar, String name) throws IOException {
        ZipEntry e = jar.getEntry(name);
        assertNotNull(e, name);
        return new String(jar.getInputStream(e).readAllBytes(), StandardCharsets.ISO_8859_1);
    }

    @Test
    void assemblesAndInstallsBundle() throws IOException {
        writeProject("    bundleName = \"Demo\"\n");
        Files.createDirectories(dir.resolve("user/plug-ins"));

        BuildResult r = run("assemble", "installPlugin");
        assertEquals(TaskOutcome.SUCCESS, r.task(":jar").getOutcome());
        assertEquals(TaskOutcome.SUCCESS, r.task(":installPlugin").getOutcome());

        Path bundle = dir.resolve("build/libs/Demo.selibrary");
        assertTrue(Files.isRegularFile(dir.resolve("build/libs/demo.jar")));
        try (JarFile jar = new JarFile(bundle.toFile())) {
            String root = text(jar, "eons-plugin");
            assertTrue(root.startsWith("#CATALOGUEID{" + ID + ":"), root);
            assertFalse(root.contains("2026-5-3-0-0-0-0"), root);
            assertTrue(root.endsWith("name = Demo\n"), root);
            assertNotNull(jar.getEntry("demo/Demo.class"));
            assertNotNull(jar.getEntry("dep/Lib.txt"));
            assertNull(jar.getEntry("module-info.class"));
            String services = text(jar, "META-INF/services/demo.Service");
            assertTrue(services.contains("demo.Impl") && services.contains("dep.Impl"), services);
        }
        assertTrue(Files.isRegularFile(dir.resolve("user/plug-ins/Demo.selibrary")));

        // a fresh timestamp on every build
        assertEquals(TaskOutcome.SUCCESS, run("sePlugin").task(":generateRootFile").getOutcome());
    }

    @Test
    void skipsInstallWithoutPluginsFolder() throws IOException {
        writeProject("");
        write("src/main/resources/eons-plugin", "name = Demo\ndemo.DemoPlugin\n");

        BuildResult r = run("installPlugin");
        assertEquals(TaskOutcome.SKIPPED, r.task(":installPlugin").getOutcome());
        assertTrue(Files.isRegularFile(dir.resolve("build/libs/demo.seext")));
    }

    @Test
    void consumersGetJarOrBundleByCapability() throws IOException {
        write("settings.gradle.kts", "include(\"lib\", \"app\")\n");
        write("lib/build.gradle.kts", """
                plugins { id("org.strangeeons.plugin") }
                group = "g"
                version = "1"
                """);
        write("lib/src/main/java/lib/Lib.java", "package lib; public class Lib {}\n");
        write("lib/src/main/resources/eons-plugin", "name = Lib\n");
        write("app/build.gradle.kts", """
                plugins { java }
                val seBundle = configurations.dependencyScope("seBundle")
                val seBundles = configurations.resolvable("seBundles") { extendsFrom(seBundle.get()) }
                dependencies {
                    implementation(project(":lib"))
                    "seBundle"(project(":lib")) { capabilities { requireCapability("g:lib-se-plugin") } }
                }
                tasks.register("show") {
                    val runtime = configurations.runtimeClasspath.get().incoming.files
                    val bundles = seBundles.get().incoming.files
                    inputs.files(runtime, bundles)
                    doLast {
                        runtime.forEach { println("RUNTIME " + it.name) }
                        bundles.forEach { println("BUNDLE " + it.name + " " + it.isFile) }
                    }
                }
                """);

        String out = run(":app:show").getOutput();
        assertTrue(out.contains("RUNTIME lib-1.jar"), out);
        assertTrue(out.contains("BUNDLE lib.selibrary true"), out);
    }
}
