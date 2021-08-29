import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm") version "1.5.30"
    id("org.jetbrains.dokka") version "1.5.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.arrow-kt:arrow-core:0.13.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
    implementation("io.arrow-kt:arrow-fx-coroutines:0.13.2")

    testImplementation("io.kotest:kotest-runner-junit5:4.6.2")
    testImplementation("io.kotest:kotest-assertions-core:4.6.2")
    testImplementation("io.kotest:kotest-property:4.6.2")
}


tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(rootDir.resolve("docs"))
    dokkaSourceSets {
        named("main") {
            moduleName.set("Saga")
//            includes.from("README.md")
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(uri("https://github.com/nomsRev/Saga/tree/master/src/main/kotlin").toURL())
                remoteLineSuffix.set("#L")
            }
        }
    }
}

tasks.clean.configure {
    doFirst {
        file("docs").listFiles()
            .filterNot { it.name == "_config.yml" }
            .forEach { delete(it.path) }
    }
}
