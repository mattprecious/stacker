package com.mattprecious.stacker.cli.upstack

import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Restack(
	private val stacker: Stacker,
) : StackerCliktCommand(shortAlias = "r") {
	override suspend fun runCommand() = stacker.upstackRestack()
}
