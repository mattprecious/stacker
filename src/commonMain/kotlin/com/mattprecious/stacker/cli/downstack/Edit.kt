package com.mattprecious.stacker.cli.downstack

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.downstack.downstackEdit

internal class Edit(
	private val stacker: StackerDeps,
) : StackerCliktCommand(shortAlias = "e") {
	override val command get() = stacker.downstackEdit()
}
