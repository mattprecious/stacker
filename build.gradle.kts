import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
	alias(libs.plugins.spotless)
	alias(libs.plugins.sqldelight)
	alias(libs.plugins.kotlin.multiplatform)
	alias(libs.plugins.kotlin.plugin.serialization)
}

repositories {
	mavenCentral()
	maven {
		setUrl("https://oss.sonatype.org/content/repositories/snapshots/")
	}
}

val buildDependenciesTask = tasks.register<BuildDependenciesTask>("buildDependencies") {
	script = layout.projectDirectory.file("build-deps.sh")
	defFile = layout.buildDirectory.file("generated/cinterop/libgit2.def")
	outputDir = layout.projectDirectory.dir("deps")
}

kotlin {
	macosArm64()
	macosX64()

	sourceSets {
		configureEach {
			languageSettings.optIn("kotlin.ExperimentalStdlibApi")
			languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
		}

		getByName("commonMain") {
			dependencies {
				implementation(libs.clikt)
				implementation(libs.kotlin.serialization.json)
				implementation(libs.ktor.client.core)
				implementation(libs.ktor.client.curl)
				implementation(libs.ktor.client.negotiation)
				implementation(libs.ktor.serialization.json)
				implementation(libs.okio)
				implementation(libs.sqldelight.driver)
			}
		}

		getByName("commonTest") {
			dependencies {
				implementation(libs.assertk)
				implementation(libs.coroutines.test)
				implementation(libs.kotlin.test)
			}
		}
	}

	targets.withType<KotlinNativeTarget>().configureEach {
		binaries.executable {
			entryPoint = "com.mattprecious.stacker.main"
		}
		compilations.getByName("main").cinterops {
			create("libgit2") {
				definitionFile.set(buildDependenciesTask.flatMap { it.defFile })
			}
		}
	}
}

sqldelight {
	databases {
		create("RepoDatabase") {
			packageName.set("com.mattprecious.stacker.db")
			dialect(libs.sqldelight.dialect)
			schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
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

abstract class BuildDependenciesTask : Exec() {
	@get:InputFile
	abstract val script: RegularFileProperty

	@get:OutputFile
	abstract val defFile: RegularFileProperty

	@get:OutputDirectory
	abstract val outputDir: DirectoryProperty

	override fun exec() {
		setExecutable(script.get().asFile)
		setArgs(
			listOf(
				"-d", defFile.get().asFile.absolutePath,
				"-b", outputDir.get().asFile.absolutePath,
			)
		)

		super.exec()
	}
}
