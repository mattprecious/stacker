package com.mattprecious.stacker.cli.branch

import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Top(
	private val stacker: Stacker,
) : StackerCliktCommand(shortAlias = "t") {
	override fun runCommand() = stacker.branchTop()
}
