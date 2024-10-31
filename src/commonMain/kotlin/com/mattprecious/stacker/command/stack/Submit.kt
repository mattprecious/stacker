package com.mattprecious.stacker.command.stack

import com.github.ajalt.clikt.core.Abort
import com.mattprecious.stacker.command.StackerMosaicCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.requireAuthenticated
import com.mattprecious.stacker.command.submit
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.stack.all
import com.mattprecious.stacker.stack.ancestors
import com.mattprecious.stacker.vc.VersionControl

internal class Submit(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val remote: Remote,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerMosaicCommand(shortAlias = "s") {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranch = stackManager.getBranch(vc.currentBranchName)
		if (currentBranch == null) {
			echo(
				message = "Cannot create a pull request from ${vc.currentBranchName.styleBranch()} since it is " +
					"not tracked. Please track with ${"st branch track".styleCode()}.",
				err = true,
			)
			throw Abort()
		}

		if (currentBranch.name == configManager.trunk || currentBranch.name == configManager.trailingTrunk) {
			echo(
				message = "Cannot create a pull request from trunk branch ${currentBranch.name.styleBranch()}.",
				err = true,
			)
			throw Abort()
		}

		requireAuthenticated(remote)

		val branchesToSubmit = (currentBranch.ancestors.toList().asReversed() + currentBranch.all)
			.filterNot { it.name == configManager.trunk || it.name == configManager.trailingTrunk }

		vc.pushBranches(branchesToSubmit.map { it.name })
		branchesToSubmit.forEach { it.submit(this@Submit, configManager, remote, stackManager, vc) }
	}
}
