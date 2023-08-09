plugins {
	application
	kotlin("jvm") version libs.versions.kotlin
	kotlin("plugin.serialization") version libs.versions.kotlin

	id("app.cash.sqldelight") version libs.versions.sqldelight

	alias(libs.plugins.spotless)
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.clikt)
	implementation(libs.github)
	implementation(libs.kotlin.serialization.json)
	implementation(libs.okio)
	implementation(libs.sqldelight.driver)
}

application {
	applicationName = "st"
	mainClass.set("com.mattprecious.stacker.StackerKt")
}

kotlin {
	jvmToolchain(17)

	compilerOptions {
		freeCompilerArgs.add("-Xcontext-receivers")
	}
}

sqldelight {
	databases {
		create("RepoDatabase") {
			packageName.set("com.mattprecious.stacker.db")
			dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.0")
		}
	}
}

spotless {
	kotlin {
		target("src/**/*.kt")
		ktlint("0.48.2").editorConfigOverride(
			mapOf(
				"ktlint_standard_filename" to "disabled",
			)
		)
	}
}

tasks.named("distTar").configure {
	enabled = false
}
tasks.named("assemble").configure {
	dependsOn(tasks.named("installDist"))
}
