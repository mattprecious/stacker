package com.mattprecious.stacker.shell

import okio.Source

interface Shell {
	fun exec(
		command: String,
		vararg args: String,
		input: Source? = null,
	): String
}
