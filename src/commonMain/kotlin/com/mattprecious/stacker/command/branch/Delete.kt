package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Delete(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "dl") {
	private val branchName: String? by argument().optional()

	override fun run() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranchName = vc.currentBranchName

		val branchName = branchName ?: currentBranchName
		if (branchName == configManager.trunk || branchName == configManager.trailingTrunk) {
			echo("Cannnot delete a trunk branch.", err = true)
			throw Abort()
		}

		val branch = stackManager.getBranch(branchName)
		if (branch != null) {
			if (branch.children.isNotEmpty()) {
				echo("Branch has children. Please retarget or untrack them.", err = true)
				throw Abort()
			}

			if (branchName == currentBranchName) {
				vc.checkout(branch.parent!!.name)
			}

			stackManager.untrackBranch(branch.value)
		}

		vc.delete(branchName)
	}
}
