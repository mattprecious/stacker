package com.mattprecious.stacker.cli.upstack

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Upstack(
	stacker: Stacker,
) : StackerCliktCommand(shortAlias = "us") {
	init {
		subcommands(
			Onto(stacker),
			Restack(stacker),
		)
	}
}
