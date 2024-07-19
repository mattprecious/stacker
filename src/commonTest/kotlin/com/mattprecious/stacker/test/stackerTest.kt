package com.mattprecious.stacker.test

import com.github.ajalt.clikt.testing.CliktCommandTestResult
import com.github.ajalt.clikt.testing.test
import com.mattprecious.stacker.remote.FakeRemote
import com.mattprecious.stacker.withStacker
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.refTo
import kotlinx.cinterop.toKString
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path
import platform.posix.chdir
import platform.posix.fgets
import platform.posix.getcwd
import platform.posix.getenv
import platform.posix.pclose
import platform.posix.popen
import platform.posix.setenv
import platform.posix.unsetenv
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlin.test.fail

fun stackerTest(
	validate: StackerTestScope.() -> Unit,
) {
	val environment = Environment()
	val fileSystem = FileSystem.SYSTEM
	val tmpPath = tmpPath("StackerTest")

	try {
		fileSystem.createDirectories(tmpPath, mustCreate = true)

		environment.withSnapshot {
			environment.workingDirectory = fileSystem.canonicalize(tmpPath).toString()
			environment.setGitNames("Stacker")
			environment.setGitEmails("stacker@example.com")
			environment.setGitDates("2020-01-01T12:00:00Z")

			StackerTestScope(environment, fileSystem).validate()
		}
	} finally {
		fileSystem.deleteRecursively(tmpPath)
	}
}

private fun tmpPath(name: String): Path {
	return FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "$name-${randomToken(16)}"
}

private fun randomToken(length: Int) = Random.nextBytes(length).toByteString(0, length).hex()

class StackerTestScope(
	val environment: Environment,
	val fileSystem: FileSystem,
) {
	val remote = FakeRemote()

	fun runStacker(vararg args: String): CliktCommandTestResult {
		var result: CliktCommandTestResult? = null
		withStacker(
			fileSystem = fileSystem,
			remoteOverride = remote,
		) {
			result = it.test(args.asList())
		}

		return result!!
	}
}

class Environment {
	var workingDirectory: String
		get() = memScoped { getcwd(null, 0.convert())!!.toKString() }
		set(value) = check(chdir(value) == 0)

	var gitAuthorDate: String? by Variable("GIT_AUTHOR_DATE")
	var gitAuthorEmail: String? by Variable("GIT_AUTHOR_EMAIL")
	var gitAuthorName: String? by Variable("GIT_AUTHOR_NAME")
	var gitCommitterDate: String? by Variable("GIT_COMMITTER_DATE")
	var gitCommitterEmail: String? by Variable("GIT_COMMITTER_EMAIL")
	var gitCommitterName: String? by Variable("GIT_COMMITTER_NAME")

	fun setGitDates(date: String) {
		gitAuthorDate = date
		gitCommitterDate = date
	}

	fun setGitEmails(email: String) {
		gitAuthorEmail = email
		gitCommitterEmail = email
	}

	fun setGitNames(name: String) {
		gitAuthorName = name
		gitCommitterName = name
	}

	/** Executes [command] and returns both the standard output and standard error. */
	fun exec(command: String): String = memScoped {
		val stream = popen("$command 2>&1", "r") ?: fail("Command ($command) failed.")

		return buildString {
			val buffer = ByteArray(4096)
			while (true) {
				val input = fgets(buffer.refTo(0), buffer.size, stream) ?: break
				append(input.toKString())
			}

			val status = pclose(stream)
			if (status != 0) {
				fail("Command ($command) failed with status: $status.")
			}
		}.trim()
	}

	/**
	 *  Captures the existing values for all the mutable properties in [Environment] and restores them
	 *  after [block] returns.
	 */
	fun withSnapshot(block: () -> Unit) {
		val snapshot = snapshot()
		try {
			block()
		} finally {
			restore(snapshot)
		}
	}

	private fun snapshot() = Snapshot(
		workingDirectory = workingDirectory,
		gitAuthorDate = gitAuthorDate,
		gitAuthorEmail = gitAuthorEmail,
		gitAuthorName = gitAuthorName,
		gitCommitterDate = gitCommitterDate,
		gitCommitterEmail = gitCommitterEmail,
		gitCommitterName = gitCommitterName,
	)

	private fun restore(snapshot: Snapshot) {
		workingDirectory = snapshot.workingDirectory
		gitAuthorDate = snapshot.gitAuthorDate
		gitAuthorEmail = snapshot.gitAuthorEmail
		gitAuthorName = snapshot.gitAuthorName
		gitCommitterDate = snapshot.gitCommitterDate
		gitCommitterEmail = snapshot.gitCommitterEmail
		gitCommitterName = snapshot.gitCommitterName
	}

	private class Variable(private val name: String) {
		operator fun getValue(thisRef: Any?, property: KProperty<*>): String? {
			return memScoped { getenv(name)?.toKString() }
		}

		operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
			if (value == null) {
				unsetenv(name)
			} else {
				setenv(name, value, 1)
			}
		}
	}

	private class Snapshot(
		val workingDirectory: String,
		val gitAuthorName: String?,
		val gitCommitterName: String?,
		val gitAuthorEmail: String?,
		val gitCommitterEmail: String?,
		val gitAuthorDate: String?,
		val gitCommitterDate: String?,
	)
}
