import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.30"
    id("org.jetbrains.dokka") version "1.5.0"
    id("java-library")
    id("maven-publish")
    id("signing")
}

group = "io.github.nomisrev"
version = "0.1.2-SNAPSHOT"

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
                remoteUrl.set(uri("https://github.com/nomisRev/Saga/tree/master/src/main/kotlin").toURL())
                remoteLineSuffix.set("#L")
            }
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

afterEvaluate {
    val sourcesJar by tasks.creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    val javadocJar by tasks.creating(Jar::class) {
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
            register("mavenJava", MavenPublication::class) {
                groupId = group.toString()
                version = version.toString()
                artifactId = "saga"

                artifact(sourcesJar)
                artifact(javadocJar)
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
                            id.set(pomDevId)
                            name.set(pomDevName)
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

        Nullable.zip(System.getenv("SIGNINGKEY"), System.getenv("SIGNINGPASSWORD")) { key, pass ->
            signing {
                useInMemoryPgpKeys(key, pass)
                sign(publishing.publications["mavenJava"])
            }
        }
    }
}

object Nullable {
    fun <A : Any, B : Any, C : Any> zip(a: A?, b: B?, f: (A, B) -> C?): C? =
        a?.let { a ->
            b?.let { b -> f(a, b) }
        }
}