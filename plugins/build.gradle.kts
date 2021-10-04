plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(gradleApi())
  implementation(kotlin("stdlib"))
  implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.5.31")
}

gradlePlugin {
  plugins {
    register("documentation") {
      id = "documentation"
      implementationClass = "io.github.nomisrev.DocumentationPlugin"
    }
    register("mpp-publish") {
      id = "mpp-publish"
      implementationClass = "io.github.nomisrev.MppPublishingPlugin"
    }
  }
}