package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.mattprecious.stacker.command.StackerMosaicCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.prettyTree
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.interactivePrompt
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Track(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val useFancySymbols: Boolean,
	private val vc: VersionControl,
) : StackerMosaicCommand(shortAlias = "tr") {
	private val branchName: String? by argument().optional()

	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val branchName = branchName ?: vc.currentBranchName
		val currentBranch = stackManager.getBranch(branchName)
		if (currentBranch != null) {
			echo(message = "Branch ${branchName.styleBranch()} is already tracked.", err = true)
			return
		}

		val trunk = configManager.trunk
		val trailingTrunk = configManager.trailingTrunk

		val defaultName = trailingTrunk ?: trunk

		val options = stackManager.getBase()!!.prettyTree(useFancySymbols = useFancySymbols) {
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
