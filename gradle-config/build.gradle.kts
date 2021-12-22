plugins {
  `kotlin-dsl`
}

repositories {
  mavenCentral()
  gradlePluginPortal()
}

dependencies {
  implementation(gradleApi())
  implementation(kotlin("stdlib"))
  implementation(libs.gradleNexus.publishPlugin)
}

gradlePlugin {
  plugins {
    register("io.github.nomisrev.mpp-publish") {
      id = "io.github.nomisrev.mpp-publish"
      displayName = "Saga publishing Gradle Config"
      implementationClass = "io.github.nomisrev.MppPublishingPlugin"
    }
  }
}