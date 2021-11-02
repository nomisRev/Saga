import io.github.nomisrev.setupDokka
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("multiplatform") version "1.5.31"
  id("io.kotest.multiplatform") version "5.0.0.5"

  id("documentation")
  id("org.jetbrains.dokka") version "1.5.31" // Auto apply from documentation

  id("io.arrow-kt.arrow-gradle-config-nexus") version "0.5.1"
  id("io.arrow-kt.arrow-gradle-config-publish-multiplatform") version "0.5.1"
}

repositories {
  mavenCentral()
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

  sourceSets {
    commonMain {
      dependencies {
        implementation(kotlin("stdlib"))
//        implementation("io.arrow-kt:arrow-core:1.0.0-SNAPSHOT")
//        implementation("io.arrow-kt:arrow-fx-coroutines:1.0.1-SNAPSHOT")
      }
    }
    commonTest {
      dependencies {
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
        implementation("io.kotest:kotest-framework-engine:5.0.0.M3")
        implementation("io.kotest:kotest-assertions-core:5.0.0.M3")
        implementation("io.kotest:kotest-property:5.0.0.M3")
      }
    }
  }
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions.jvmTarget = "1.8"
  sourceCompatibility = "1.8"
  targetCompatibility = "1.8"
}

setupDokka(
  outputDirectory = rootDir.resolve("docs"),
  name = "Saga",
  baseUrl = "https://github.com/nomisRev/Saga/tree/master",
  paths = listOf("README.MD")
)
