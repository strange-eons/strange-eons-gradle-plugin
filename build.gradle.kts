plugins {
    `java-gradle-plugin`
    jacoco
}

group = "org.strangeeons.gradle"
version = "0.1.0-SNAPSHOT"
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

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}
