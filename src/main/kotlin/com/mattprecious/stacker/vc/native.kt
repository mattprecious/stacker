package com.mattprecious.stacker.vc

import java.io.FileNotFoundException
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.notExists

@Suppress("UnsafeDynamicallyLoadedCode") // Only loading from our own JAR contents.
internal fun loadLibGit2() {
	val osName = System.getProperty("os.name").lowercase(Locale.US)
	val osArch = System.getProperty("os.arch").lowercase(Locale.US)
	val libraryRelativePath = if (osName.contains("mac")) {
		"$osArch/libgit2.dylib"
	} else {
		throw IllegalStateException("Unsupported OS: $osName")
	}

	val overridePath = System.getProperty("stacker.library.path")
	val nativeDir = if (overridePath != null) {
		Path.of(overridePath)
	} else {
		Path.of(GitVersionControl::class.java.protectionDomain.codeSource.location.path)
			.parent // Libs folder.
			.parent // App folder.
			.resolve("native")
	}

	val libraryPath = nativeDir.resolve(libraryRelativePath)
	if (libraryPath.notExists()) {
		throw FileNotFoundException("Unable to locate $libraryPath")
	}
	System.load(libraryPath.toAbsolutePath().toString())
}
