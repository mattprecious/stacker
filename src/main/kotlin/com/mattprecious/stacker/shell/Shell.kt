package com.mattprecious.stacker.shell

import okio.Source

interface Shell {
	fun exec(
		command: String,
		vararg args: String,
		input: Source? = null,
		suppressErrors: Boolean = false,
	): String

	fun execStatus(
		command: String,
		vararg args: String,
	): Boolean
}
