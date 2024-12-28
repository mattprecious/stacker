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

internal class Create(
	configManager: ConfigManager,
	locker: Locker,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCliktCommand(shortAlias = "c") {
	private val branchName by argument()

	override val command by lazy {
		CreateCommand(
			branchName = branchName,
			configManager = configManager,
			locker = locker,
			stackManager = stackManager,
			vc = vc,
		)
	}
}

internal class CreateCommand(
	private val branchName: String,
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranch = stackManager.getBranch(vc.currentBranchName)
		if (currentBranch == null) {
			printStaticError(
				buildAnnotatedString {
					append("Cannot branch from ")
					branch { append(vc.currentBranchName) }
					append(" since it is not tracked. Please track with ")
					code { append("st branch track") }
					append(".")
				},
			)
			abort()
		}

		vc.createBranchFromCurrent(branchName)
		stackManager.trackBranch(branchName, currentBranch.name, vc.getSha(currentBranch.name))
	}
}
