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
