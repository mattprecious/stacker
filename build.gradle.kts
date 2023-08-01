plugins {
	application
	kotlin("jvm") version libs.versions.kotlin
	kotlin("plugin.serialization") version libs.versions.kotlin

	alias(libs.plugins.spotless)
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.clikt)
	implementation(libs.kotlin.serialization.json)
	implementation(libs.okio)
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
