package com.mattprecious.stacker.cli.branch

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.branch.branchUp

internal class Up(
	private val stacker: StackerDeps,
) : StackerCliktCommand(shortAlias = "u") {
	override val command get() = stacker.branchUp()
}
