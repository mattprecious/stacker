package com.mattprecious.stacker.command.upstack

import com.github.ajalt.clikt.core.Abort
import com.mattprecious.stacker.command.StackerMosaicCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.perform
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.stack.all
import com.mattprecious.stacker.vc.VersionControl

internal class Restack(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerMosaicCommand(shortAlias = "r") {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranchName = vc.currentBranchName
		val currentBranch = stackManager.getBranch(currentBranchName)
		if (currentBranch == null) {
			echo(
				message = "Cannot restack ${currentBranchName.styleBranch()} since it is not tracked. " +
					"Please track with ${"st branch track".styleCode()}.",
				err = true,
			)
			throw Abort()
		}

		val operation = Locker.Operation.Restack(
			startingBranch = currentBranch.name,
			currentBranch.all.map { it.name }.toList(),
		)

		locker.beginOperation(operation) {
			operation.perform(this@Restack, this@beginOperation, stackManager, vc)
		}
	}
}
