package com.mattprecious.stacker.shell

import okio.Source
import okio.buffer
import okio.sink
import okio.source
import java.util.concurrent.TimeUnit

class RealShell : Shell {
	override fun exec(
		command: String,
		vararg args: String,
		input: Source?,
	): String {
		val process = ProcessBuilder(command, *args)
			.redirectError(ProcessBuilder.Redirect.INHERIT)
			.start()

		if (input != null) {
			process.outputStream.sink().buffer().use { it.writeAll(input) }
		}

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
