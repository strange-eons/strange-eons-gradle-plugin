# strange-eons-gradle-plugin

A Gradle plugin that packages a JVM project, or a script plug-in's project
folder, as a Strange Eons plug-in bundle (`.selibrary`, `.seext`, `.seplugin`,
`.setheme`) alongside the plain JAR.

## Tasks

- **`generateRootFile`** copies the `eons-plugin` root file, stamping each
  catalog ID with the build time (UUID kept) so SE treats every build as an
  update.
- **`generateVersionProperties`** writes the version properties file, if
  `versionProperties` is set.
- **`sePlugin`** builds the bundle: main classes and resources, the plug-in
  folder, the generated root and version files, and the runtime classpath,
  with `META-INF/services` files merged. Part of `assemble`.
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

## Script plug-ins

A plug-in made of scripts and resources keeps them in an SE project folder
(`eons-plugin`, `resources/`, ...). Point `pluginFolder` at it:

```kotlin
plugins { id("org.strangeeons.plugin") }

version = "10.0.1"

strangeEonsPlugin {
    pluginFolder = file("MyGame")
    versionProperties = "resources/mygame/version.properties"
}
```

The folder is packed as SE's *Make Bundle* command packs it: hidden files,
`seproject`, `*.ts.js` and SE's default excluded names (`Thumbs.db`, `.git*`,
...) are left out. `rootFile` defaults to the folder's `eons-plugin`, and
`bundleType` to the type *Make Bundle* would guess, reading the plug-in
script.

`versionProperties` adds a properties file at that path in the bundle, with
`version` (the project version) and `se-version` (the float SE plug-ins
report: `10.0.1` is `10.0`). The project version must be
`major.minor.patch[-SNAPSHOT]` with a one-digit minor, so that `10.10` can't
read as `10.1`.

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
repository. To release, run `./gradlew release` on a clean, pushed `main`: it
sets the release version in `gradle.properties`, tags it (e.g. `1.0.0`) and
bumps to the next snapshot. Nothing is pushed; review, then
`git push --follow-tags`. The tag push releases that version to Maven
Central. Both run in the `maven-central` environment, which holds
`MAVEN_CENTRAL_USERNAME` and `MAVEN_CENTRAL_PASSWORD` (a Central Portal user
token), plus `SIGNING_KEY` (ASCII-armored private key) and
`SIGNING_KEY_PASSWORD`.

## License

MIT
