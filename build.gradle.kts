import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotest.multiplatform)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.arrowGradleConfig.multiplatform)
  alias(libs.plugins.arrowGradleConfig.nexus)
  alias(libs.plugins.arrowGradleConfig.publishMultiplatform)
  alias(libs.plugins.arrowGradleConfig.formatter)
  alias(libs.plugins.githooks)
  alias(libs.plugins.dokka)
  alias(libs.plugins.kover)
}

allprojects {
  repositories {
    mavenCentral()
  }
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.kotlin.stdlibCommon)
        implementation(libs.arrow.core)
        implementation(libs.arrow.fx)
      }
    }
    commonTest {
      dependencies {
        implementation(libs.coroutines.core)
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
  }
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions.jvmTarget = "1.8"
  sourceCompatibility = "1.8"
  targetCompatibility = "1.8"
}

gitHooks {
  setHooks(
    mapOf(
      "pre-commit" to "spotlessApply",
      "pre-push" to "spotlessCheck"
    )
  )
}

infix fun <T> Property<T>.by(value: T) {
  set(value)
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
      skipDeprecated by true
      includes.from("README.MD")
      sourceLink {
        localDirectory.set(project.file("src/commonMain/kotlin"))
        remoteUrl.set(project.uri("https://github.com/nomisRev/Saga/tree/master/src/commonMain/kotlin").toURL())
        remoteLineSuffix.set("#L")
      }
    }
  }
}

nexusPublishing {
  repositories {
    named("sonatype") {
      nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
      snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
    }
  }
}
