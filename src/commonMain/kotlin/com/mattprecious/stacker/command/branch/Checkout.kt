package com.mattprecious.stacker.command.branch

import androidx.compose.runtime.remember
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.mattprecious.stacker.command.StackerCliktCommand
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
	configManager: ConfigManager,
	locker: Locker,
	stackManager: StackManager,
	useFancySymbols: Boolean,
	vc: VersionControl,
) : StackerCliktCommand(shortAlias = "co") {
	private val branchName: String? by argument().optional()

	override val command by lazy {
		BranchCheckout(
			branchName = branchName,
			configManager = configManager,
			locker = locker,
			stackManager = stackManager,
			useFancySymbols = useFancySymbols,
			vc = vc,
		)
	}
}

internal class BranchCheckout(
	private val branchName: String?,
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val useFancySymbols: Boolean,
	private val vc: VersionControl,
) : StackerCommand() {
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
			branchName
		} else {
			printStaticError("'$branchName' does not match any branches known to git.")
			abort()
		}

		vc.checkout(branch)
	}
}
