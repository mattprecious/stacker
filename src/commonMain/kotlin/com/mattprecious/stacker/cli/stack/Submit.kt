package com.mattprecious.stacker.cli.stack

import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Submit(
	private val stacker: Stacker,
) : StackerCliktCommand(shortAlias = "s") {
	override suspend fun runCommand() = stacker.stackSubmit()
}
