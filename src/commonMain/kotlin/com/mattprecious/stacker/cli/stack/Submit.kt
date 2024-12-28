package com.mattprecious.stacker.cli.stack

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.stack.stackSubmit

internal class Submit(
	private val stacker: StackerDeps,
) : StackerCliktCommand(shortAlias = "s") {
	override val command get() = stacker.stackSubmit()
}
