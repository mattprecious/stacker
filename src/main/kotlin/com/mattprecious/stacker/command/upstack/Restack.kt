package com.mattprecious.stacker.command.upstack

import com.github.ajalt.clikt.core.Abort
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.error
import com.mattprecious.stacker.flattenUp
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.perform
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Restack(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "r") {
	override fun run() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranchName = vc.currentBranchName
		val currentBranch = stackManager.getBranch(currentBranchName)
		if (currentBranch == null) {
			error(
				message = "Cannot restack ${currentBranchName.styleBranch()} since it is not tracked. " +
					"Please track with ${"st branch track".styleCode()}.",
			)
			throw Abort()
		}

		val operation = Locker.Operation.Restack(
			startingBranch = currentBranch.name,
			currentBranch.flattenUp().map { it.name },
		)

		locker.beginOperation(operation) {
			operation.perform(stackManager, vc)
		}
	}
}
