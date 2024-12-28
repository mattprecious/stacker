package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.command.StackerCliktCommand
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.rendering.code
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Rename(
	configManager: ConfigManager,
	locker: Locker,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCliktCommand(shortAlias = "rn") {
	private val newName by argument()

	override val command by lazy {
		RenameCommand(
			newName = newName,
			configManager = configManager,
			locker = locker,
			stackManager = stackManager,
			vc = vc,
		)
	}
}

internal class RenameCommand(
	private val newName: String,
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranchName = vc.currentBranchName
		val currentBranch = stackManager.getBranch(currentBranchName)
		if (currentBranch == null) {
			printStaticError(
				buildAnnotatedString {
					append("Cannot rename ")
					branch { append(currentBranchName) }
					append(" since it is not tracked. Please track with ")
					code { append("st branch track") }
					append(".")
				},
			)
			abort()
		}

		vc.renameBranch(branchName = currentBranch.name, newName = newName)
		stackManager.renameBranch(currentBranch.value, newName)
	}
}
