import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.file.Files
import java.security.MessageDigest

plugins {
	application

	alias(libs.plugins.spotless)
	alias(libs.plugins.sqldelight)
	alias(libs.plugins.kotlin.jvm)
	alias(libs.plugins.kotlin.plugin.serialization)

	id("java-test-fixtures")
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.clikt)
	implementation(libs.jline.terminal)
	implementation(libs.kotlin.serialization.json)
	implementation(libs.ktor.client.cio)
	implementation(libs.ktor.client.core)
	implementation(libs.ktor.client.negotiation)
	implementation(libs.ktor.serialization.json)
	implementation(libs.okio)
	implementation(libs.sqldelight.driver)
	implementation(libs.slf4j.simple)

	testImplementation(libs.assertk)
	testImplementation(libs.junit)
}

application {
	applicationName = "st"
	mainClass.set("com.mattprecious.stacker.StackerKt")

	applicationDefaultJvmArgs = listOf("--enable-preview", "--enable-native-access=ALL-UNNAMED")

	applicationDistribution.from(projectDir) {
		// libgit and related dependencies are built locally and dropped into this folder for local development. CI has been
		// configured to drop them here instead of src/main/dist for parity.
		include("native/**")
	}
}

kotlin {
	jvmToolchain(21)

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

tasks.named("distZip").configure {
	enabled = false
}
tasks.named("assemble").configure {
	dependsOn(tasks.named("installDist"))
}

tasks.withType<JavaExec>().all {
	jvmArgs("--enable-preview")
	// Override the library path for tasks like run so that they can find the libraries.
	systemProperty("stacker.library.path", file("native").absolutePath)
}

tasks.withType<JavaCompile>().all {
	options.compilerArgs!! += "--enable-preview"
}

tasks.withType<Test>().all {
	jvmArgs("--enable-preview", "--enable-native-access=ALL-UNNAMED")
	systemProperty("stacker.library.path", file("native").absolutePath)
}

val requireNativeTask by tasks.register("requireNativeFiles") {
	outputs.upToDateWhen { false }

	doFirst {
		check(file("native").walk().any { it.isFile }) {
			"'native' directory does not exist or is empty. This must be populated with libgit libraries.\n" +
				"If developing locally, please run .github/workflows/build-deps.sh"
		}
	}
}

tasks.withType<KotlinCompile>().all {
	dependsOn(requireNativeTask)
}

private data class DownloadInfo(
	val url: String,
	val checksum: String,
)

val downloadJExtractTask = tasks.register<DownloadJExtractTask>("downloadJExtract") {
	val os = DefaultNativePlatform.getCurrentOperatingSystem()
	val downloadInfo = when {
		os.isMacOsX -> DownloadInfo(
			url = "https://download.java.net/java/early_access/jextract/1/openjdk-21-jextract+1-2_macos-x64_bin.tar.gz",
			checksum = "6183f3d079ed531cc5a332e6d86c0abfbc5d001f1e85f721ebc5232204c987a2",
		)

		os.isLinux -> DownloadInfo(
			url = "https://download.java.net/java/early_access/jextract/1/openjdk-21-jextract+1-2_linux-x64_bin.tar.gz",
			checksum = "83626610b1b074bfe4985bd825d8ba44d906a30b24c42d971b6ac836c7eb0671",
		)

		else -> throw IllegalStateException("Unsupported OS: ${os.name}.")
	}

	url = downloadInfo.url
	checksum = downloadInfo.checksum

	outputFile = layout.buildDirectory.file("jextract/jextract.tar.gz")
}

val extractJExtractTask = tasks.register<Copy>("extractJExtract") {
	from(tarTree(downloadJExtractTask.flatMap { it.outputFile }))
	into(layout.buildDirectory.dir("jextract"))
}

val generateBindingsTask = tasks.register<GenerateBindingsTask>("generateBindings") {
	jExtractDir = extractJExtractTask.map { it.destinationDir }
	libgit2IncludeDir = layout.projectDirectory.dir("libgit2/include")
	outputDir = layout.buildDirectory.dir("generated/libgit2")
}

java {
	sourceSets.main.configure {
		java.srcDir(generateBindingsTask)
	}
}

abstract class DownloadJExtractTask : DefaultTask() {
	@get:Input
	abstract val url: Property<String>

	@get:Input
	abstract val checksum: Property<String>

	@get:OutputFile
	abstract val outputFile: RegularFileProperty

	@TaskAction
	fun download() {
		val filePath = outputFile.get().asFile.toPath()

		HttpClient.newHttpClient().send(
			HttpRequest.newBuilder(URI.create(url.get())).build(),
			BodyHandlers.ofFile(filePath),
		)

		val expectedHash = checksum.get()

		val bytes = Files.readAllBytes(filePath)
		val actualHash = BigInteger(1, MessageDigest.getInstance("SHA-256").digest(bytes)).toString(16)
		check(actualHash == expectedHash) {
			"""
				Checksum does not match.
				URL: ${url.get()}
				Expected checksum: $expectedHash
				Actual checksum: $actualHash
				""".trimIndent()
		}
	}
}

abstract class GenerateBindingsTask : Exec() {
	@get:InputDirectory
	abstract val jExtractDir: DirectoryProperty

	@get:InputDirectory
	abstract val libgit2IncludeDir: DirectoryProperty

	@get:OutputDirectory
	abstract val outputDir: DirectoryProperty

	override fun exec() {
		setExecutable(jExtractDir.file("jextract-21/bin/jextract").get().asFile)
		setArgs(
			listOf(
				"--source",
				"--output", outputDir.get().asFile.absolutePath,
				"-t", "com.github",
				"-I", "libgit2/include/",
				"libgit2/include/git2.h",
			)
		)

		super.exec()
	}
}
