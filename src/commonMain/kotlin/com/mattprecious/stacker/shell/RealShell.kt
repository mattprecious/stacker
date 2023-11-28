package com.mattprecious.stacker.shell

import platform.posix.system

class RealShell : Shell {
	override fun exec(
		command: String,
		vararg args: String,
	) {
		system("$command ${args.joinToString(" ")}")
	}
}
