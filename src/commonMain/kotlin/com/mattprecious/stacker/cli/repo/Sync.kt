package com.mattprecious.stacker.cli.repo

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.repo.repoSync

internal class Sync(
	private val stacker: StackerDeps,
) : StackerCliktCommand(shortAlias = "s") {
	override val command get() = stacker.repoSync()
}
