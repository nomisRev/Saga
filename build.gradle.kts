import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    kotlin("jvm") version "1.5.30"
    id("org.jetbrains.dokka") version "1.5.0"
    `java-library`
    `maven-publish`
    signing
}

group = "io.github.nomisrev"
version = "0.1.0"

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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<DokkaTask>().configureEach {
    outputDirectory.set(rootDir.resolve("docs"))
    dokkaSourceSets {
        named("main") {
            perPackageOption {
                matchingRegex.set(".*\\.internal.*") // will match all .internal packages and sub-packages
                suppress.set(true)
            }
            skipDeprecated.set(true)
            moduleName.set("Saga")
            includes.from("README.md")
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(uri("https://github.com/nomsRev/Saga/tree/master/src/main/kotlin").toURL())
                remoteLineSuffix.set("#L")
            }
        }
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn.add(tasks.javadoc)
    archiveClassifier.set("javadoc")
    from(tasks.javadoc)
}

artifacts {
    archives(sourcesJar)
    archives(javadocJar)
    archives(tasks.jar)
}

val pomDevId = "nomisrev"
val pomDevName = "Simon Vergauwen"
val releaseRepo = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
val snapshotRepo = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group.toString()
            version = version.toString()
            artifactId = "saga"

            artifact(sourcesJar.get())
            artifact(javadocJar.get())
            from(components["java"])

            pom {
                name.set("Saga")
                packaging = "jar"
                description.set("Functional implementation of Saga pattern in Kotlin on top of Coroutines")
                url.set("https://github.com/nomisrev/Saga")

                scm {
                    url.set("https://github.com/nomisrev/Saga")
                    connection.set("scm:git:git://github.com/nomisrev/Saga.git")
                    developerConnection.set("scm:git:ssh://git@github.com/nomisrev/Saga.git")
                }
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("nomisrev")
                        name.set("Simon Vergauwen")
                    }
                }
            }
        }

    }
    repositories {
        maven {
            credentials {
                username = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_PWD")
            }

            url = if (version.toString().endsWith("SNAPSHOT")) snapshotRepo else releaseRepo
        }
    }

    // Guide: https://docs.gradle.org/current/userguide/signing_plugin.html
    if (project.hasProperty("SIGNINGKEY") && project.hasProperty("SIGNINGPASSWORD")) {
        signing {
            val signingKey: String? by project
            val signingPassword: String? by project
            useInMemoryPgpKeys(signingKey, signingPassword)

            sign(publishing.publications["mavenJava"])
        }
    }
}
