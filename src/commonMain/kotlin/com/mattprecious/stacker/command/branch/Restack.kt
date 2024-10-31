package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.mattprecious.stacker.command.StackerMosaicCommand
import com.mattprecious.stacker.command.perform
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Restack(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerMosaicCommand(shortAlias = "r") {
	private val branchName: String? by argument().optional()

	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranchName = vc.currentBranchName
		val branchName = branchName ?: currentBranchName
		if (stackManager.getBranch(currentBranchName) == null) {
			echo(
				message = "Cannot restack ${currentBranchName.styleBranch()} since it is not tracked. " +
					"Please track with ${"st branch track".styleCode()}.",
				err = true,
			)
			throw Abort()
		}

		val operation = Locker.Operation.Restack(
			startingBranch = currentBranchName,
			branches = listOf(branchName),
		)

		locker.beginOperation(operation) {
			operation.perform(this@Restack, this@beginOperation, stackManager, vc)
		}
	}
}
