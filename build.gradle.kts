import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  alias(libs.plugins.kotest.multiplatform)
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.arrowGradleConfig.kotlin)
  alias(libs.plugins.arrowGradleConfig.formatter)
  alias(libs.plugins.arrowGradleConfig.publish)
  alias(libs.plugins.arrowGradleConfig.nexus)
  alias(libs.plugins.arrowGradleConfig.versioning)
  alias(libs.plugins.dokka)
  alias(libs.plugins.detekt)
  alias(libs.plugins.kover)
}

infix fun <T> Property<T>.by(value: T) {
  set(value)
}

allprojects {
  extra.set("dokka.outputDirectory", rootDir.resolve("docs"))
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions.jvmTarget = "1.8"
  sourceCompatibility = "1.8"
  targetCompatibility = "1.8"
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

detekt {
  buildUponDefaultConfig = true
  allRules = true
}

tasks.withType<Detekt>().configureEach {
  reports {
    html.required by true
    sarif.required by true
    txt.required by false
    xml.required by false
  }
}
