package com.mattprecious.stacker.command.upstack

import com.github.ajalt.clikt.core.Abort
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.flattenUp
import com.mattprecious.stacker.command.perform
import com.mattprecious.stacker.command.prettyTree
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.error
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.interactivePrompt
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Onto(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "o") {
	override fun run() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranchName = vc.currentBranchName
		val currentBranch = stackManager.getBranch(currentBranchName)
		if (currentBranch == null) {
			error(
				message = "Cannot retarget ${currentBranchName.styleBranch()} since it is not tracked. " +
					"Please track with ${"st branch track".styleCode()}.",
			)
			throw Abort()
		}

		if (currentBranchName == configManager.trunk || currentBranchName == configManager.trailingTrunk) {
			error(message = "Cannot retarget a trunk branch.")
			throw Abort()
		}

		val options = stackManager.getBase()!!.prettyTree { it.name != currentBranchName }
		val newParent = interactivePrompt(
			message = "Select the parent branch for ${currentBranchName.styleBranch()}",
			options = options,
			default = options.find { it.branch.name == currentBranch.parent!!.name },
			displayTransform = { it.pretty },
			valueTransform = { it.branch.name },
		).branch

		stackManager.updateParent(currentBranch, newParent)

		val operation = Locker.Operation.Restack(
			startingBranch = currentBranch.name,
			currentBranch.flattenUp().map { it.name },
		)

		locker.beginOperation(operation) {
			operation.perform(stackManager, vc)
		}
	}
}
