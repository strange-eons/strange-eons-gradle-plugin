# strange-eons-gradle-plugin

A Gradle plugin that packages a JVM project as a Strange Eons plug-in bundle
(`.selibrary`, `.seext`, `.seplugin`, `.setheme`) alongside the plain JAR.

## Tasks

- **`generateRootFile`** copies the `eons-plugin` root file, stamping each
  catalog ID with the build time (UUID kept) so SE treats every build as an
  update.
- **`sePlugin`** builds the bundle: main classes and resources, the generated
  root file, and the runtime classpath, with `META-INF/services` files merged.
  Part of `assemble`.
- **`installPlugin`** copies the bundle into the SE user `plug-ins` folder,
  if that folder exists.

## Usage

```kotlin
// settings.gradle.kts
pluginManagement {
    includeBuild("../strange-eons-gradle-plugin")
}

// build.gradle.kts
plugins {
    `java-library`
    id("org.strangeeons.plugin")
}

dependencies {
    compileOnly("ca.cgjennings:strange-eons-script-api:1.0-SNAPSHOT")
    implementation("org.example:bundled-library:1.0")  // shaded into the bundle
}

strangeEonsPlugin {
    bundleName = "MyGame"                             // default: project name
    bundleType = "seext"                              // default: guessed from the root file
    rootFile = file("src/main/resources/eons-plugin") // default
    userFolder = file("/path/to/StrangeEons3")        // default: SE's user folder
}
```

The bundle is written to `build/libs/<bundleName>.<bundleType>`.

`bundleType` defaults to the type SE's *Make Bundle* command would guess: a
root file with no plug-in line is a library, a class name containing
`theme` is a theme, anything else is an extension.

`userFolder` defaults to `STRANGE_EONS_USER_DIR`, else `%APPDATA%\StrangeEons3`
on Windows and `~/.StrangeEons3` elsewhere.

To install on every build:

```kotlin
tasks.assemble { finalizedBy(tasks.installPlugin) }
```

## Consuming the bundle

Project dependencies resolve to the plain JAR, which is also what
`maven-publish` publishes. The bundle is offered as the `sePluginElements`
variant, which is not published; select it by requiring the
`<group>:<name>-se-plugin` capability:

```kotlin
val seBundle = configurations.dependencyScope("seBundle")
val seBundles = configurations.resolvable("seBundles") { extendsFrom(seBundle.get()) }

dependencies {
    "seBundle"("org.example:my-library") {
        capabilities { requireCapability("org.example:my-library-se-plugin") }
    }
}
```

## Build

```
./gradlew test
```

## Publishing

Pushes to `main` publish a `-SNAPSHOT` to the Maven Central snapshot
repository. Pushing a `v<version>` tag releases that version to Maven Central.
Both run in the `maven-central` environment, which holds
`MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` (a Central Portal user
token), plus `SIGNING_KEY` (ASCII-armored private key) and
`SIGNING_KEY_PASSWORD`.

## License

MIT
