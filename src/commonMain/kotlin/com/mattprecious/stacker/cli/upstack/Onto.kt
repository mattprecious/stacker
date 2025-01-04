package com.mattprecious.stacker.cli.upstack

import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Onto(
	private val stacker: Stacker,
) : StackerCliktCommand(shortAlias = "o") {
	override suspend fun runCommand() = stacker.upstackOnto()
}
