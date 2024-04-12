package com.mattprecious.stacker.test

import com.mattprecious.stacker.remote.FakeRemote
import com.mattprecious.stacker.withStacker
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.toKString
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.ForwardingFileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.posix.chdir
import platform.posix.getcwd
import kotlin.random.Random

fun stackerTest(
	validate: StackerTestScope.() -> Unit,
) {
	val fileSystem: FileSystem = TempFileSystem("StackerTest")
	val root = "/".toPath()

	try {
		fileSystem.createDirectories(root, mustCreate = true)

		// Grab the current directory so that we can change back before exiting.
		val startingDirectory = memScoped { getcwd(null, 0.convert())!!.toKString() }
		try {
			// Note: This chdir is only necessary because we're currently shelling out for git push/pull. If that moves back
			// to using libgit then we can remove this and rely only on the wrapped file system.
			chdir(fileSystem.canonicalize(root).toString())

			StackerTestScope(fileSystem).validate()
		} finally {
			chdir(startingDirectory)
		}
	} finally {
		fileSystem.deleteRecursively(root)
	}
}

class StackerTestScope(
	val fileSystem: FileSystem,
) {
	val remote = FakeRemote()

	fun run(vararg args: String): Int {
		return withStacker(
			fileSystem = fileSystem,
			remoteOverride = remote,
		) {
			it.parse(args.asList())
		}
	}
}

/** Creates a temporary folder and transforms all operations to be relative to this folder. */
private class TempFileSystem(name: String) : ForwardingFileSystem(FileSystem.SYSTEM) {
	private val base = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "$name-${randomToken(16)}"

	private fun randomToken(length: Int) = Random.nextBytes(length).toByteString(0, length).hex()

	override fun onPathParameter(path: Path, functionName: String, parameterName: String): Path {
		val forwardingPath = if (path.isRelative) "/$path".toPath() else path

		return base.resolve(forwardingPath.relativeTo(ABSOLUTE_ROOT))
	}

	override fun onPathResult(path: Path, functionName: String): Path {
		return path.relativeTo(base)
	}

	companion object {
		private val ABSOLUTE_ROOT = "/".toPath()
	}
}
