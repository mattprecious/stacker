package com.mattprecious.stacker.command.downstack

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.output.TermUi
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.flattenDown
import com.mattprecious.stacker.command.perform
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.interactivePrompt
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.stack.StackManager
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

		val downstack = currentBranch.flattenDown(configManager).map { it.name }
		val trunk = downstack.last()

		val downstackWithoutTrunk = downstack.dropLast(1)

		val downstackWithComments = downstack.toMutableList().apply {
			removeLast()
			add("# $trunk (trunk)")
		}

		val result = TermUi.editText(
			text = downstackWithComments.joinToString("\n"),
			editor = vc.editor,
			requireSave = true,
		) ?: return

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
				displayTransform = { it.render(trunk) },
				valueTransform = { it.render(trunk) },
			)

			val branch = stackManager.getBranch(branchName)!!

			when (action) {
				RemovedOption.Cancel -> return
				RemovedOption.Untrack -> stackManager.untrackBranch(branch)
				RemovedOption.Remove -> {
					stackManager.updateParent(stackManager.getBranch(branchName)!!, stackManager.getBranch(trunk)!!)
				}
				RemovedOption.Delete -> {
					if (branchName == currentBranchName) {
						vc.checkout(branch.parent!!.name)
					}

					stackManager.untrackBranch(branch)
					vc.delete(branchName)
				}
			}
		}

		newStack.windowed(size = 2, step = 1, partialWindows = true).forEach {
			val branch = stackManager.getBranch(it.first())!!
			val parent = stackManager.getBranch(it.getOrNull(1) ?: trunk)!!
			stackManager.updateParent(branch = branch, parent = parent)
		}

		val operation = Locker.Operation.Restack(
			startingBranch = vc.currentBranchName,
			newStack.reversed(),
		)

		locker.beginOperation(operation) {
			operation.perform(stackManager, vc)
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
