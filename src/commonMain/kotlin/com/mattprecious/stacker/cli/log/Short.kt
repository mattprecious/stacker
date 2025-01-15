package com.mattprecious.stacker.cli.log

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.log.logShort

internal class Short(
	private val stacker: StackerDeps,
) : StackerCliktCommand(shortAlias = "s") {
	override val command get() = stacker.logShort()
}
