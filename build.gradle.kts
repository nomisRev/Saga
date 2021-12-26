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

infix fun <T> Property<T>.by(value: T) {
  set(value)
}

tasks {
  withType<DokkaTask>().configureEach {
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

  withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
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
    jvmTest {
      dependencies {
        implementation(libs.kotest.runnerJUnit5)
      }
    }
  }
}
