package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.parameters.arguments.argument
import com.mattprecious.stacker.command.StackerMosaicCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Create(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerMosaicCommand(shortAlias = "c") {
	private val branchName by argument()

	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranch = stackManager.getBranch(vc.currentBranchName)
		if (currentBranch == null) {
			echo(
				message = "Cannot branch from ${vc.currentBranchName.styleBranch()} since it is not tracked. " +
					"Please track with ${"st branch track".styleCode()}.",
				err = true,
			)
			throw Abort()
		}

		vc.createBranchFromCurrent(branchName)
		stackManager.trackBranch(branchName, currentBranch.name, vc.getSha(currentBranch.name))
	}
}
