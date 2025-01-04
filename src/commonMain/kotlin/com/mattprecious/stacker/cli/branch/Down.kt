package com.mattprecious.stacker.cli.branch

import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Down(
	private val stacker: Stacker,
) : StackerCliktCommand(shortAlias = "d") {
	override suspend fun runCommand() = stacker.branchDown()
}
