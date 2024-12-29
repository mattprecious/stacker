package com.mattprecious.stacker.cli.repo

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Repo(
	stacker: Stacker,
) : StackerCliktCommand(shortAlias = "r") {
	init {
		subcommands(
			Init(stacker),
			Sync(stacker),
		)
	}
}
