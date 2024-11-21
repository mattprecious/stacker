package com.mattprecious.stacker.command.downstack

import com.github.ajalt.clikt.core.Abort
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.perform
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.interactivePrompt
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.stack.ancestors
import com.mattprecious.stacker.vc.VersionControl

internal class Edit(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand(shortAlias = "e") {
	override fun run() {
		val currentBranchName = vc.currentBranchName
		val currentBranch = stackManager.getBranch(currentBranchName)
		if (currentBranch == null) {
			echo(
				message = "Cannot edit downstack since ${currentBranchName.styleBranch()} is not tracked. " +
					"Please track with ${"st branch track".styleCode()}.",
				err = true,
			)
			throw Abort()
		}

		val trunk = configManager.trunk
		val trailingTrunk = configManager.trailingTrunk

		val downstack = (sequenceOf(currentBranch) + currentBranch.ancestors).map { it.name }.toList()
		val firstTrunkIndex = downstack.indexOfFirst { it == trunk || it == trailingTrunk }

		val downstackTrunk = downstack[firstTrunkIndex]
		val downstackWithoutTrunk = downstack.take(firstTrunkIndex)

		val downstackWithComments = downstackWithoutTrunk + "# $downstackTrunk (trunk)"

		val result = Editor(
			editorPath = vc.editor,
			requireSave = true,
			extension = ".txt",
		).edit(downstackWithComments.joinToString("\n")) ?: return

		val newStack = result.lines().filter { !it.startsWith('#') && it.isNotBlank() }

		val addedBranches = newStack.subtract(downstackWithoutTrunk.toSet())
		if (addedBranches.isNotEmpty()) {
			echo(
				message = "Inserting new branches is not supported: $addedBranches.",
				err = true,
			)
			throw Abort()
		}

		val removedBranches = downstackWithoutTrunk.subtract(newStack.toSet())
		removedBranches.forEach { branchName ->
			val action = interactivePrompt(
				"${branchName.styleBranch()} was removed from the list. What would you like to do?",
				options = RemovedOption.entries,
				filteringEnabled = false,
				displayTransform = { it.render(downstackTrunk) },
				valueTransform = { it.render(downstackTrunk) },
			)

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
			operation.perform(this@Edit, this@beginOperation, stackManager, vc)
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
