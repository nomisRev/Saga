rootProject.name = "saga"

plugins {
    id("com.gradle.enterprise") version "3.7"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
    }
}
