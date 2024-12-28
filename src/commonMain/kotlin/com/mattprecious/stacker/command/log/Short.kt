package com.mattprecious.stacker.command.log

import com.mattprecious.stacker.command.StackerCliktCommand
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.parentSha
import com.mattprecious.stacker.command.prettyTree
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Short(
	configManager: ConfigManager,
	stackManager: StackManager,
	useFancySymbols: Boolean,
	vc: VersionControl,
) : StackerCliktCommand(shortAlias = "s") {
	override val command by lazy {
		ShortCommand(
			configManager = configManager,
			stackManager = stackManager,
			useFancySymbols = useFancySymbols,
			vc = vc,
		)
	}
}

internal class ShortCommand(
	private val configManager: ConfigManager,
	private val stackManager: StackManager,
	private val useFancySymbols: Boolean,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		val trunk = configManager.trunk
		val trailingTrunk = configManager.trailingTrunk

		stackManager.getBase()?.prettyTree(
			selected = stackManager.getBranch(vc.currentBranchName),
			useFancySymbols = useFancySymbols,
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
		}?.let(::printStatic)
	}
}
