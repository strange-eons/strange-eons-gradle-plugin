plugins {
    `java-gradle-plugin`
    id("com.vanniktech.maven.publish") version "0.37.0"
    id("net.researchgate.release") version "3.2.0"
    jacoco
}

group = "org.strangeeons.gradle"
description = "Packages a JVM project as a Strange Eons plug-in bundle."

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.4.1")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    plugins {
        create("strangeEonsPlugin") {
            id = "org.strangeeons.plugin"
            implementationClass = "org.strangeeons.gradle.plugin.StrangeEonsPlugin"
            displayName = "Strange Eons plug-in bundle"
            description = project.description
        }
    }
}

mavenPublishing {
    publishToMavenCentral()
    // Snapshots publish unsigned; signing applies only when a key is supplied.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    pom {
        name = rootProject.name
        description = project.description
        inceptionYear = "2026"
        url = "https://github.com/strange-eons/strange-eons-gradle-plugin"
        licenses {
            license {
                name = "MIT License"
                url = "https://opensource.org/licenses/MIT"
                distribution = "repo"
            }
        }
        developers {
            developer {
                id = "christophercurrie"
                name = "Christopher Currie"
                url = "https://github.com/christophercurrie"
            }
        }
        scm {
            url = "https://github.com/strange-eons/strange-eons-gradle-plugin"
            connection = "scm:git:https://github.com/strange-eons/strange-eons-gradle-plugin.git"
            developerConnection = "scm:git:ssh://git@github.com/strange-eons/strange-eons-gradle-plugin.git"
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:all,-missing", "-quiet")
}
