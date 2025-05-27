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
	id("com.gradle.develocity") version ("4.0.2")
}

develocity {
	buildScan {
		termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
		termsOfUseAgree = "yes"
		if (System.getenv("CI") == "true") {
			tag("CI")
		} else {
			publishing.onlyIf { false }
		}
	}
}
