package com.mattprecious.stacker.cli.stack

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Stack(
	stacker: StackerDeps,
) : StackerCliktCommand(shortAlias = "s") {
	init {
		subcommands(
			Submit(stacker),
		)
	}
}
