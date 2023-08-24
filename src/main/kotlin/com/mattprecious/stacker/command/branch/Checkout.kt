package com.mattprecious.stacker.command.branch

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.prettyTree
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.error
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.interactivePrompt
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Checkout(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "co") {
	private val branchName: String? by argument().optional()

	override fun run() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val branch = if (branchName == null) {
			val options = stackManager.getBase()!!.prettyTree()
			interactivePrompt(
				message = "Checkout a branch",
				options = options,
				default = options.find { it.branch.name == vc.currentBranchName },
				displayTransform = { it.pretty },
				valueTransform = { it.branch.name },
			).branch.name
		} else if (vc.branches.contains(branchName)) {
			branchName!!
		} else {
			error("'$branchName' does not match any branches known to git.")
			throw Abort()
		}

		vc.checkout(branch)
	}
}
