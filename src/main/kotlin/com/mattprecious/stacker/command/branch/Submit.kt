package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.core.Abort
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.requireAuthenticated
import com.mattprecious.stacker.command.submit
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.error
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Submit(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val remote: Remote,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "s") {
	override fun run() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranch = stackManager.getBranch(vc.currentBranchName)
		if (currentBranch == null) {
			error(
				message = "Cannot create a pull request from ${vc.currentBranchName.styleBranch()} since it is " +
					"not tracked. Please track with ${"st branch track".styleCode()}.",
			)
			throw Abort()
		}

		if (currentBranch.name == configManager.trunk || currentBranch.name == configManager.trailingTrunk) {
			error(
				message = "Cannot create a pull request from trunk branch ${currentBranch.name.styleBranch()}.",
			)
			throw Abort()
		}

		remote.requireAuthenticated()

		vc.pushBranches(listOf(currentBranch.name))
		currentBranch.submit(configManager, remote, vc)
	}
}
