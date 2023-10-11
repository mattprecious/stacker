plugins {
	application

	alias(libs.plugins.spotless)
	alias(libs.plugins.sqldelight)
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.plugin.serialization)
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(files("libs/libgit2j.jar"))

	implementation(libs.clikt)
	implementation(libs.github)
	implementation(libs.jline.terminal)
	implementation(libs.kotlin.serialization.json)
	implementation(libs.okio)
	implementation(libs.sqldelight.driver)
}

application {
	applicationName = "st"
	mainClass.set("com.mattprecious.stacker.StackerKt")

	applicationDefaultJvmArgs = listOf("--enable-preview", "--enable-native-access=ALL-UNNAMED")
}

kotlin {
	jvmToolchain(20)

	compilerOptions {
		freeCompilerArgs.add("-Xcontext-receivers")
	}
}

sqldelight {
	databases {
		create("RepoDatabase") {
			packageName.set("com.mattprecious.stacker.db")
			dialect(libs.sqldelight.dialect)
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

tasks.withType<JavaExec>().all {
	jvmArgs("--enable-preview")
	// Override the library path for tasks like run and test so they can find the libraries.
	systemProperty("stacker.library.path", file("src/main/dist/native").absolutePath)
}

tasks.withType<JavaCompile>().all {
	options.compilerArgs!! += "--enable-preview"
}
