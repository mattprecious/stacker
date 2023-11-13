package com.mattprecious.stacker.shell

interface Shell {
	fun exec(
		command: String,
		vararg args: String,
	)
}
