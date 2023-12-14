rootProject.name = "stacker"

pluginManagement {
	repositories {
		gradlePluginPortal()
		mavenCentral()
		maven {
			setUrl("https://oss.sonatype.org/content/repositories/snapshots/")
		}
	}
}

plugins {
	id("com.gradle.enterprise") version ("3.16.1")
}

gradleEnterprise {
	buildScan {
		termsOfServiceUrl = "https://gradle.com/terms-of-service"
		termsOfServiceAgree = "yes"
		if (System.getenv("CI") == "true") {
			publishAlways()
			tag("CI")
		}
	}
}
