package com.mattprecious.stacker.command.branch

import androidx.compose.runtime.remember
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.prettyTree
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.InteractivePrompt
import com.mattprecious.stacker.rendering.PromptState
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal class Checkout(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val useFancySymbols: Boolean,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "co") {
	private val branchName: String? by argument().optional()

	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val branch = if (branchName == null) {
			val options = stackManager.getBase()!!.prettyTree(useFancySymbols = useFancySymbols)
			if (options.size == 1) {
				options.single().branch.name
			} else {
				render { onResult ->
					InteractivePrompt(
						message = "Checkout a branch",
						state = remember {
							PromptState(
								options = options,
								default = options.find { it.branch.name == vc.currentBranchName },
								displayTransform = { it.pretty },
								valueTransform = { it.branch.name },
							)
						},
						onSelected = { onResult(it.branch.name) },
					)
				}
			}
		} else if (vc.branches.contains(branchName)) {
			branchName!!
		} else {
			echo("'$branchName' does not match any branches known to git.", err = true)
			throw Abort()
		}

		vc.checkout(branch)
	}
}
