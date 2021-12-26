package io.github.nomisrev

import io.github.gradlenexus.publishplugin.NexusPublishExtension
import io.github.gradlenexus.publishplugin.NexusPublishPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

/**
 * An empty [Plugin] that exposes a set of utility functions to simplify setting up MPP publishing.
 */
class MppPublishingPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.plugins.apply(MavenPublishPlugin::class.java)
    target.plugins.apply(SigningPlugin::class.java)
    target.plugins.apply(NexusPublishPlugin::class.java)
    target.setupPublishing()
    target.configureNexus()
  }
}

fun Project.configureNexus() {
  configure<NexusPublishExtension> {
    repositories {
      sonatype {
        username by ossUser
        password by ossToken
        stagingProfileId by ossStagingProfileId
        nexusUrl by repositoryRelease?.let(::uri)
        snapshotRepositoryUrl by repositorySnapshot?.let(::uri)
      }
    }
  }
}

fun Project.setupPublishing() {
  afterEvaluate {
    extensions.getByType(PublishingExtension::class.java).apply {
      val mavenPublications = publications.withType<MavenPublication>()
      mavenPublications.all {
        artifact(project.javadocJar())

        pom {
          name by pomName
          description by pomDescription
          url by pomUrl

          licenses {
            license {
              name by pomLicenseName
              url by pomLicenseUrl
            }
          }
          developers {
            developer {
              id by pomDeveloperId
              name by pomDeveloperName
              email by pomDeveloperEmail
            }
          }
          scm {
            url by pomSmcUrl
            connection by pomSmcConnection
            developerConnection by pomSmcDeveloperConnection
          }

          if (pomUrl?.startsWith("https://github.com") == true) issueManagement {
            system by "GitHub"
            this@issueManagement.url by "$pomUrl/issues"
          }
        }
      }
      repositories {
        val repository = if (version.toString().endsWith("SNAPSHOT")) repositorySnapshot else repositoryRelease
        requireNotNull(repository) { "No maven repository specified. Gradle property repository.snapshot, or repository.release needs to be available" }
        maven {
          name = "Maven"
          url = uri(repository)
          credentials {
            username = System.getenv("SONATYPE_USER")
            password = System.getenv("SONATYPE_PWD")
          }
        }
      }

      Nullable.zip(
        System.getenv("SIGNINGKEY"), System.getenv("SIGNINGPASSWORD")
      ) { key, pass -> signPublications(key, pass) }
    }
  }
}

object Nullable {
  fun <A : Any, B : Any, C : Any> zip(a: A?, b: B?, f: (A, B) -> C?): C? = a?.let { aa ->
    b?.let { bb -> f(aa, bb) }
  }
}

fun Project.signPublications(
  key: String, pass: String
) {
  val publications = project.extensions.getByType(PublishingExtension::class.java).publications

  project.extensions.getByType(SigningExtension::class.java).apply { useInMemoryPgpKeys(key, pass) }.sign(publications)
}

/* We either try to find the existing javadocJar, or we register an empty javadocJar task */
fun Project.javadocJar(): TaskProvider<Jar> {
  val taskName = "javadocJar"
  return try {
    tasks.named(name = taskName)
  } catch (e: UnknownTaskException) {
    tasks.register(name = taskName) {
      archiveClassifier by "javadoc"
    }
  }
}

val Project.pomName: String?
  get() = getVariable("pom.name", "POM_NAME")

val Project.pomDescription: String?
  get() = getVariable("pom.description", "POM_DESCRIPTION")

val Project.pomUrl: String?
  get() = getVariable("pom.url", "POM_URL")

val Project.pomLicenseName: String?
  get() = getVariable("pom.license.name", "POM_LICENSE_NAME")

val Project.pomLicenseUrl: String?
  get() = getVariable("pom.license.url", "POM_LICENSE_URL")

val Project.pomDeveloperId: String?
  get() = getVariable("pom.developer.id", "POM_DEVELOPER_ID")

val Project.pomDeveloperName: String?
  get() = getVariable("pom.developer.name", "POM_DEVELOPER_NAME")

val Project.pomDeveloperEmail: String?
  get() = getVariable("pom.developer.email", "POM_DEVELOPER_EMAIL")

val Project.pomSmcUrl: String?
  get() = getVariable("pom.smc.url", "POM_SMC_URL")

val Project.pomSmcConnection: String?
  get() = getVariable("pom.smc.connection", "POM_SMC_CONNECTION")

val Project.pomSmcDeveloperConnection: String?
  get() = getVariable("pom.smc.developerConnection", "POM_SMC_DEVELOPER_CONNECTION")

val Project.repositorySnapshot: String?
  get() = getVariable("repository.snapshot", "REPOSITORY_SNAPSHOT")

val Project.repositoryRelease: String?
  get() = getVariable("repository.release", "REPOSITORY_RELEASE")

val Project.ossUser: String?
  get() = getVariable("oss.user", "OSS_USER")

val Project.ossToken: String?
  get() = getVariable("oss.token", "OSS_TOKEN")

val Project.ossStagingProfileId: String?
  get() = getVariable("oss.stagingProfileId", "OSS_STAGING_PROFILE_ID")
