package com.mattprecious.stacker.command.downstack

import androidx.compose.runtime.remember
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.perform
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.InteractivePrompt
import com.mattprecious.stacker.rendering.PromptState
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.rendering.code
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.stack.ancestors
import com.mattprecious.stacker.vc.VersionControl
import kotlinx.collections.immutable.toPersistentList

fun StackerDeps.downstackEdit(): StackerCommand {
	return DownstackEdit(
		configManager = configManager,
		locker = locker,
		stackManager = stackManager,
		vc = vc,
	)
}

internal class DownstackEdit(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		val currentBranchName = vc.currentBranchName
		val currentBranch = stackManager.getBranch(currentBranchName)
		if (currentBranch == null) {
			printStaticError(
				buildAnnotatedString {
					append("Cannot edit downstack since ")
					branch { append(currentBranchName) }
					append(" is not tracked. Please track with ")
					code { append("st branch track") }
					append(".")
				},
			)
			abort()
		}

		val trunk = configManager.trunk
		val trailingTrunk = configManager.trailingTrunk

		val downstack = (sequenceOf(currentBranch) + currentBranch.ancestors).map { it.name }.toList()
		val firstTrunkIndex = downstack.indexOfFirst { it == trunk || it == trailingTrunk }

		val downstackTrunk = downstack[firstTrunkIndex]
		val downstackWithoutTrunk = downstack.take(firstTrunkIndex)

		val downstackWithComments = downstackWithoutTrunk + "# $downstackTrunk (trunk)"

		val result = Editor(
			commandScope = this,
			editorPath = vc.editor,
			requireSave = true,
			extension = ".txt",
		).edit(downstackWithComments.joinToString("\n")) ?: return

		val newStack = result.lines().filter { !it.startsWith('#') && it.isNotBlank() }

		val addedBranches = newStack.subtract(downstackWithoutTrunk.toSet())
		if (addedBranches.isNotEmpty()) {
			printStaticError(
				"Inserting new branches is not supported: $addedBranches.",
			)
			abort()
		}

		val removedBranches = downstackWithoutTrunk.subtract(newStack.toSet())
		removedBranches.forEach { branchName ->
			val action = render { onResult ->
				InteractivePrompt(
					message = buildAnnotatedString {
						branch { append(branchName) }
						append(" was removed from the list. What would you like to do?")
					},
					filteringEnabled = false,
					state = remember {
						PromptState(
							RemovedOption.entries.toPersistentList(), default = null,
							displayTransform = { it.render(downstackTrunk) },
							valueTransform = { it.render(downstackTrunk) },
						)
					},
					onSelected = { onResult(it) },
				)
			}

			val branch = stackManager.getBranch(branchName)!!

			when (action) {
				RemovedOption.Cancel -> return
				RemovedOption.Untrack -> stackManager.untrackBranch(branch.value)
				RemovedOption.Remove -> {
					stackManager.updateParent(
						branch = stackManager.getBranch(branchName)!!.value,
						parent = stackManager.getBranch(downstackTrunk)!!.value,
					)
				}
				RemovedOption.Delete -> {
					if (branchName == currentBranchName) {
						vc.checkout(branch.parent!!.name)
					}

					stackManager.untrackBranch(branch.value)
					vc.delete(branchName)
				}
			}
		}

		newStack.windowed(size = 2, step = 1, partialWindows = true).forEach {
			val branch = stackManager.getBranch(it.first())!!
			val parent = stackManager.getBranch(it.getOrNull(1) ?: downstackTrunk)!!
			stackManager.updateParent(branch = branch.value, parent = parent.value)
		}

		val operation = Locker.Operation.Restack(
			startingBranch = vc.currentBranchName,
			newStack.reversed(),
		)

		locker.beginOperation(operation) {
			operation.perform(this@work, this@beginOperation, stackManager, vc)
		}
	}

	private fun RemovedOption.render(trunkName: String): String = when (this) {
		RemovedOption.Remove -> "Remove from stack, set parent to $trunkName"
		RemovedOption.Untrack -> "Untrack it"
		RemovedOption.Delete -> "Delete it"
		RemovedOption.Cancel -> "Cancel"
	}

	private enum class RemovedOption {
		Remove,
		Untrack,
		Delete,
		Cancel,
	}
}
