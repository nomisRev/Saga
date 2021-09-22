import io.github.nomisrev.setupPublishing
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("multiplatform") version "1.5.31"
  id("io.kotest.multiplatform") version "5.0.0.5"
  id("org.jetbrains.dokka") version "1.5.30"

  id("maven-publish")
  id("signing")
  id("mpp-publish")
}

group = "io.github.nomisrev"
version = "0.0.1000"

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
//        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
//        implementation("io.arrow-kt:arrow-core:1.0.0-SNAPSHOT")
//        implementation("io.arrow-kt:arrow-fx-coroutines:1.0.1-SNAPSHOT")
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

setupPublishing(
  pomDevId = "nomisRev",
  pomDevName = "Simon Vergauwen",
  projectUrl = "https://github.com/nomisrev/Saga",
  projectDesc = "Functional implementation of Saga pattern in Kotlin on top of Coroutines",
  releaseRepo = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"),
  snapshotRepo = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"),
)
