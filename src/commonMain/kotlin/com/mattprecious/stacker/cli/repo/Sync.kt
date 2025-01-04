package com.mattprecious.stacker.cli.repo

import com.mattprecious.stacker.Stacker
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Sync(
	private val stacker: Stacker,
) : StackerCliktCommand(shortAlias = "s") {
	override suspend fun runCommand() = stacker.repoSync()
}
