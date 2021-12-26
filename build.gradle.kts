import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.kotest.multiplatform)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.arrowGradleConfig.kotlin)
  alias(libs.plugins.arrowGradleConfig.formatter)
  id("io.github.nomisrev.mpp-publish")
  alias(libs.plugins.dokka)
  alias(libs.plugins.kover)
}

tasks.withType<DokkaTask>().configureEach {
  outputDirectory by rootDir.resolve("docs")
  moduleName by "Saga"
  dokkaSourceSets.apply {
    named("commonMain") {
      perPackageOption {
        matchingRegex.set(".*\\.internal.*")
        suppress by true
      }
      includes.from("README.md")
      skipDeprecated by true
      sourceLink {
        localDirectory.set(project.file("src/commonMain/kotlin"))
        remoteUrl.set(project.uri("https://github.com/nomisRev/Saga/tree/master/src/commonMain/kotlin").toURL())
        remoteLineSuffix.set("#L")
      }
    }
  }
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlin.stdlibCommon)
        implementation(libs.coroutines.core)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.coroutines.core)
        implementation(libs.arrow.core)
        implementation(libs.arrow.fx)
        implementation(libs.kotest.frameworkEngine)
        implementation(libs.kotest.assertionsCore)
        implementation(libs.kotest.property)
      }
    }
    named("jvmTest") {
      dependencies {
        implementation(libs.kotest.runnerJUnit5)
      }
    }

    val commonMain by getting
    val mingwX64Main by getting
    val linuxX64Main by getting
    val iosArm32Main by getting
    val iosArm64Main by getting
    val iosSimulatorArm64Main by getting
    val iosX64Main by getting
    val macosArm64Main by getting
    val macosX64Main by getting
    val tvosArm64Main by getting
    val tvosSimulatorArm64Main by getting
    val tvosX64Main by getting
    val watchosArm32Main by getting
    val watchosArm64Main by getting
    val watchosSimulatorArm64Main by getting
    val watchosX64Main by getting
    val watchosX86Main by getting
    val nativeMain by getting

    nativeMain.dependsOn(commonMain)
    mingwX64Main.dependsOn(nativeMain)
    linuxX64Main.dependsOn(nativeMain)
    iosArm32Main.dependsOn(nativeMain)
    iosArm64Main.dependsOn(nativeMain)
    iosSimulatorArm64Main.dependsOn(nativeMain)
    iosX64Main.dependsOn(nativeMain)
    macosArm64Main.dependsOn(nativeMain)
    macosX64Main.dependsOn(nativeMain)
    tvosArm64Main.dependsOn(nativeMain)
    tvosSimulatorArm64Main.dependsOn(nativeMain)
    tvosX64Main.dependsOn(nativeMain)
    watchosArm32Main.dependsOn(nativeMain)
    watchosArm64Main.dependsOn(nativeMain)
    watchosSimulatorArm64Main.dependsOn(nativeMain)
    watchosX64Main.dependsOn(nativeMain)
    watchosX86Main.dependsOn(nativeMain)
  }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions.jvmTarget = "1.8"
  sourceCompatibility = "1.8"
  targetCompatibility = "1.8"
}

infix fun <T> Property<T>.by(value: T) {
  set(value)
}
