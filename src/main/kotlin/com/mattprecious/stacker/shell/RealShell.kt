package com.mattprecious.stacker.shell

import okio.buffer
import okio.source
import java.util.concurrent.TimeUnit

class RealShell : Shell {
	override fun exec(
		command: String,
		vararg args: String,
	): String {
		val process = ProcessBuilder(command, *args).run {
			redirectError(ProcessBuilder.Redirect.INHERIT)
		}.start()

		check(process.waitFor(20, TimeUnit.SECONDS)) {
			"Process $command took more than 20 seconds"
		}

		val exitValue = process.exitValue()
		check(exitValue == 0) {
			"Process $command exited: $exitValue"
		}

		return process.inputStream.source().buffer().use { it.readUtf8().trim() }
	}
}
