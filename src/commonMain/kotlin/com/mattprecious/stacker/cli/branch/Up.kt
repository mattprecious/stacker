package com.mattprecious.stacker.cli.branch

import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Up(
	private val stacker: Stacker,
) : StackerCliktCommand(shortAlias = "u") {
	override fun runCommand() = stacker.branchUp()
}
