package com.mattprecious.stacker.cli.branch

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.branch.branchSubmit

internal class Submit(
	private val stacker: StackerDeps,
) : StackerCliktCommand(shortAlias = "s") {
	override val command get() = stacker.branchSubmit()
}
