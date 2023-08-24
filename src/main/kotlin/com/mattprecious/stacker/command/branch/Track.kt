package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.prettyTree
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.error
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.interactivePrompt
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Track(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "tr") {
	private val branchName: String? by argument().optional()

	override fun run() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val branchName = branchName ?: vc.currentBranchName
		val currentBranch = stackManager.getBranch(branchName)
		if (currentBranch != null) {
			error(message = "Branch ${branchName.styleBranch()} is already tracked.")
			return
		}

		val trunk = configManager.trunk
		val trailingTrunk = configManager.trailingTrunk

		val defaultName = trailingTrunk ?: trunk

		val options = stackManager.getBase()!!.prettyTree {
			it.name == trunk || it.name == trailingTrunk || vc.isAncestor(
				branchName = branchName,
				possibleAncestorName = it.name,
			)
		}
		val parent = interactivePrompt(
			message = "Select the parent branch for ${branchName.styleBranch()}",
			options = options,
			default = options.find { it.branch.name == defaultName },
			displayTransform = { it.pretty },
			valueTransform = { it.branch.name },
		).branch.name

		val parentSha = vc.getSha(parent)

		stackManager.trackBranch(branchName, parent, parentSha)
	}
}
