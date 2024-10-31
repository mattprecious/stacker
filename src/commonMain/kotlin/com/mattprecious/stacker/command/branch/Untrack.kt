package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.mattprecious.stacker.command.StackerMosaicCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Untrack(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerMosaicCommand(shortAlias = "ut") {
	private val branchName: String? by argument().optional()

	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val branchName = branchName ?: vc.currentBranchName
		val currentBranch = stackManager.getBranch(branchName)
		if (currentBranch == null) {
			echo(message = "Branch ${branchName.styleBranch()} is already not tracked.", err = true)
			return
		}

		if (currentBranch.children.isNotEmpty()) {
			echo(
				message = "Branch ${branchName.styleBranch()} has children. Please retarget or untrack them.",
				err = true,
			)
			return
		}

		stackManager.untrackBranch(currentBranch.value)
	}
}
