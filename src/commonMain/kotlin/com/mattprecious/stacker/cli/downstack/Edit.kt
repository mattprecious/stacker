package com.mattprecious.stacker.cli.downstack

import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Edit(
	private val stacker: Stacker,
) : StackerCliktCommand(shortAlias = "e") {
	override suspend fun runCommand() = stacker.downstackEdit()
}
