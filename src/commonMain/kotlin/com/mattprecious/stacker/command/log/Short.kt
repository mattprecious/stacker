package com.mattprecious.stacker.command.log

import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.parentSha
import com.mattprecious.stacker.command.prettyTree
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Short(
	private val configManager: ConfigManager,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "s") {
	override fun run() {
		requireInitialized(configManager)
		val trunk = configManager.trunk
		val trailingTrunk = configManager.trailingTrunk

		echo(
			stackManager.getBase()?.prettyTree(
				selected = stackManager.getBranch(vc.currentBranchName),
			)?.joinToString("\n") {
				val needsRestack = run needsRestack@{
					val parent = it.branch.parent ?: return@needsRestack false
					val parentSha = vc.getSha(parent.name)
					return@needsRestack if (it.branch.name == trunk || it.branch.name == trailingTrunk) {
						false
					} else {
						it.branch.parentSha != parentSha ||
							!vc.isAncestor(branchName = it.branch.name, possibleAncestorName = parent.name)
					}
				}

				if (needsRestack) {
					"${it.pretty} (needs restack)"
				} else {
					it.pretty
				}
			},
		)
	}
}
