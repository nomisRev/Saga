package io.github.nomisrev

import org.gradle.api.provider.Property
import org.gradle.api.Project

infix fun <T> Property<T>.by(value: T?) {
  set(value)
}

internal fun Project.getVariable(propertyName: String, environmentVariableName: String): String? {
  val property: String? = project.properties[propertyName]?.toString()
  val environmentVariable: String? = System.getenv(environmentVariableName)
  return property ?: environmentVariable
}