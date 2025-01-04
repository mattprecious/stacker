package com.mattprecious.stacker.cli.log

import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Short(
	private val stacker: Stacker,
) : StackerCliktCommand(shortAlias = "s") {
	override suspend fun runCommand() = stacker.logShort()
}
