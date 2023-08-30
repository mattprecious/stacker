package com.mattprecious.stacker.shell

import okio.buffer
import okio.source

class RealShell : Shell {
	override fun exec(
		command: String,
		vararg args: String,
	): String {
		val process = ProcessBuilder(command, *args).run {
			redirectError(ProcessBuilder.Redirect.INHERIT)
		}.start()

		process.waitFor()

		val exitValue = process.exitValue()
		check(exitValue == 0) {
			"Process $command exited: $exitValue"
		}

		return process.inputStream.source().buffer().use { it.readUtf8().trim() }
	}
}
