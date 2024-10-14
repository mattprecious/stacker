package com.mattprecious.stacker.command.branch

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.jakewharton.mosaic.runMosaicBlocking
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.prettyTree
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.InteractivePrompt
import com.mattprecious.stacker.rendering.LocalPrinter
import com.mattprecious.stacker.rendering.PromptState
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl
import kotlinx.coroutines.awaitCancellation

internal class Checkout(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "co") {
	private val branchName: String? by argument().optional()

	private sealed interface State {
		data object Starting : State
		data object Prompting : State
		data class CheckingOut(val branchName: String) : State
	}

	override fun run() {
		runMosaicBlocking {
			var state by remember { mutableStateOf<State>(State.Starting) }

			LaunchedEffect(state) {
				when (val s = state) {
					State.Starting -> {
						requireInitialized(configManager)
						requireNoLock(locker)

						state = when {
							branchName == null -> State.Prompting
							vc.branches.contains(branchName) -> State.CheckingOut(branchName!!)
							else -> {
								echo("'$branchName' does not match any branches known to git.", err = true)
								throw Abort()
							}
						}
					}

					State.Prompting -> awaitCancellation()
					is State.CheckingOut -> vc.checkout(s.branchName)
				}
			}

			LocalPrinter.current.Messages()

			if (state == State.Prompting) {
				val options = remember { stackManager.getBase()!!.prettyTree() }
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
					onSelected = { state = State.CheckingOut(it.branch.name) },
				)
			}
		}
	}
}
