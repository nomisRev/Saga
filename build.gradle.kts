import java.net.URI
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("multiplatform") version "1.5.31"
  id("io.kotest.multiplatform") version "5.0.0.5"
  id("org.jetbrains.dokka") version "1.5.30"
  id("maven-publish")
  id("signing")
}

group = "io.github.nomisrev"
version = "0.1.4"

repositories {
  mavenCentral()
  maven(url = "https://oss.sonatype.org/content/repositories/snapshots/")
}

kotlin {
  explicitApi()
  jvm()
  js(IR) {
    browser()
    nodejs()
  }
  linuxX64()
  mingwX64()
  macosX64()
  macosArm64()
  tvos()
  tvosSimulatorArm64()
  watchosArm32()
  watchosX86()
  watchosX64()
  watchosSimulatorArm64()
  iosX64()
  iosArm64()
  iosArm32()
  iosSimulatorArm64()

  targets.all {
    compilations.all {
      kotlinOptions {
        verbose = true
      }
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(kotlin("stdlib"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
        implementation("io.arrow-kt:arrow-core:1.0.0-SNAPSHOT")
        implementation("io.arrow-kt:arrow-fx-coroutines:1.0.1-SNAPSHOT")
      }
    }
    commonTest {
      dependencies {
        implementation("io.kotest:kotest-framework-engine:5.0.0.M1")
        implementation("io.kotest:kotest-assertions-core:5.0.0.M1")
        implementation("io.kotest:kotest-property:5.0.0.M1")
      }
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

tasks.withType<DokkaTask>().configureEach {
  outputDirectory.set(rootDir.resolve("docs"))
  dokkaSourceSets {
    named("commonMain") {
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
  kotlinOptions.jvmTarget = "1.8"
  sourceCompatibility = "1.8"
  targetCompatibility = "1.8"
}

afterEvaluate {

  val pomDevId = "nomisrev"
  val pomDevName = "Simon Vergauwen"
  val releaseRepo = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
  val snapshotRepo = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")

  publishing {
    val mavenPublications = publications.withType<MavenPublication>()
    mavenPublications.all {
      artifact(project.tasks.emptyJavadocJar())
      setupPom(
        gitUrl = "https://github.com/nomisrev/Saga.git",
        url = "https://github.com/nomisrev/Saga",
        description = "Functional implementation of Saga pattern in Kotlin on top of Coroutines",
        pomDevId = pomDevId,
        pomDevName = pomDevName
      )
    }
    repositories {
      maven(if (version.toString().endsWith("SNAPSHOT")) snapshotRepo else releaseRepo)
    }
    signPublications()
  }
}

object Nullable {
  fun <A : Any, B : Any, C : Any> zip(a: A?, b: B?, f: (A, B) -> C?): C? =
    a?.let { aa ->
      b?.let { bb -> f(aa, bb) }
    }
}

fun Project.signPublications() {
  Nullable.zip(System.getenv("SIGNINGKEY"), System.getenv("SIGNINGPASSWORD")) { key, pass ->
    signing {
      useInMemoryPgpKeys(key, pass)
      sign(publishing.publications)
    }
  }
}

fun TaskContainer.emptyJavadocJar(): TaskProvider<Jar> {
  val taskName = "javadocJar"
  return try {
    named(name = taskName)
  } catch (e: UnknownTaskException) {
    register(name = taskName) { archiveClassifier.set("javadoc") }
  }
}

fun MavenPublication.setupPom(
  gitUrl: String,
  url: String,
  description: String,
  pomDevId: String,
  pomDevName: String,
  licenseName: String = "The Apache Software License, Version 2.0",
  licenseUrl: String = "https://www.apache.org/licenses/LICENSE-2.0.txt"
) = pom {
  if (!name.isPresent) {
    name.set(artifactId)
  }
  this@pom.description.set(description)
  this@pom.url.set(url)
  licenses {
    license {
      name.set(licenseName)
      this@license.url.set(licenseUrl)
    }
  }
  developers {
    developer {
      id.set(pomDevId)
      name.set(pomDevName)
    }
  }
  scm {
    connection.set(gitUrl)
    developerConnection.set(gitUrl)
    this@scm.url.set(url)
  }
  if (gitUrl.startsWith("https://github.com")) issueManagement {
    system.set("GitHub")
    this@issueManagement.url.set(gitUrl.replace(".git", "/issues"))
  }
}

fun RepositoryHandler.maven(
  uri: URI,
  sonatypeUsername: String? = System.getenv("SONATYPE_USER"),
  sonatypePassword: String? = System.getenv("SONATYPE_PWD"),
): MavenArtifactRepository = maven {
  name = "Maven"
  url = uri
  credentials {
    username = sonatypeUsername
    password = sonatypePassword
  }
}
