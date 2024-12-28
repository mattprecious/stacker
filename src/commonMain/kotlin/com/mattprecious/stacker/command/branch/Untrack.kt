package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.command.StackerCliktCommand
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Untrack(
	configManager: ConfigManager,
	locker: Locker,
	stackManager: StackManager,
	vc: VersionControl,
) : StackerCliktCommand(shortAlias = "ut") {
	private val branchName: String? by argument().optional()

	override val command by lazy {
		UntrackCommand(
			branchName = branchName,
			configManager = configManager,
			locker = locker,
			stackManager = stackManager,
			vc = vc,
		)
	}
}

internal class UntrackCommand(
	private val branchName: String?,
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val branchName = branchName ?: vc.currentBranchName
		val currentBranch = stackManager.getBranch(branchName)
		if (currentBranch == null) {
			printStaticError(
				buildAnnotatedString {
					append("Branch ")
					branch { append(branchName) }
					append(" is already not tracked.")
				},
			)
			return
		}

		if (currentBranch.children.isNotEmpty()) {
			printStaticError(
				buildAnnotatedString {
					append("Branch ")
					branch { append(branchName) }
					append(" has children. Please retarget or untrack them.")
				},
			)
			return
		}

		stackManager.untrackBranch(currentBranch.value)
	}
}
