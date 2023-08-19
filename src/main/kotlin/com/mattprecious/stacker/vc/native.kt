package com.mattprecious.stacker.vc

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.Locale

@Suppress("UnsafeDynamicallyLoadedCode") // Only loading from our own JAR contents.
internal fun loadLibGit2() {
	val osName = System.getProperty("os.name").lowercase(Locale.US)
	val osArch = System.getProperty("os.arch").lowercase(Locale.US)
	val nativeLibraryJarPath = if (osName.contains("mac")) {
		"/jni/$osArch/libgit2.dylib"
	} else {
		throw IllegalStateException("Unsupported OS: $osName")
	}

	val nativeLibraryUrl = GitVersionControl::class.java.getResource(nativeLibraryJarPath)
		?: throw IllegalStateException("Unable to read $nativeLibraryJarPath from JAR")

	val nativeLibraryFile: Path
	try {
		nativeLibraryFile = Files.createTempFile("libgit2", null)

		// File-based deleteOnExit() uses a special internal shutdown hook that always runs last.
		nativeLibraryFile.toFile().deleteOnExit()
		nativeLibraryUrl.openStream().use { nativeLibrary ->
			Files.copy(nativeLibrary, nativeLibraryFile, StandardCopyOption.REPLACE_EXISTING)
		}
	} catch (e: IOException) {
		throw RuntimeException("Unable to extract native library from JAR", e)
	}

	System.load(nativeLibraryFile.toAbsolutePath().toString())
}
