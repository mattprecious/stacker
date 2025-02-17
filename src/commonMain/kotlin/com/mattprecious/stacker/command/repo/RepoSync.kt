package com.mattprecious.stacker.command.repo

import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.collections.TreeNode
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.rendering.YesNoPrompt
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.repoSync(
	forceAskToDelete: Boolean,
): StackerCommand {
	return RepoSync(
		forceAskToDelete = forceAskToDelete,
		configManager = configManager,
		remote = remote,
		stackManager = stackManager,
		vc = vc,
	)
}

internal class RepoSync(
	private val forceAskToDelete: Boolean,
	private val configManager: ConfigManager,
	private val remote: Remote,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		val trunk = configManager.trunk!!
		val trailingTrunk = configManager.trailingTrunk

		vc.pull(trunk)
		trailingTrunk?.let(vc::pull)

		stackManager.getBase()!!.offerBranchDeletion(this) {
			it.name != trunk &&
				it.name != trailingTrunk &&
				(forceAskToDelete || !it.value.hasAskedToDelete)
		}
	}

	/**
	 * Recursively checks the status of any remote PRs associated with this branch and offers to
	 * delete the branch locally if PR is in a terminal state.
	 */
	private suspend fun TreeNode<Branch>.offerBranchDeletion(
		commandScope: StackerCommandScope,
		filter: (TreeNode<Branch>) -> Boolean,
	) {
		if (filter(this)) {
			val status = remote.getPrStatus(name)
			val prompt = when (status) {
				Remote.PrStatus.Closed -> "closed"
				Remote.PrStatus.Merged -> "merged"
				Remote.PrStatus.NotFound,
				Remote.PrStatus.Open,
				-> return
			}

			val delete = commandScope.render { onResult ->
				YesNoPrompt(
					message = buildAnnotatedString {
						append("PR for ")
						branch { append(name) }
						append(" has been $prompt, would you like to delete it?")
					},
					default = null,
					onSubmit = { onResult(it) },
				)
			}

			if (delete == true) {
				if (vc.currentBranchName == name) {
					// Our original parent might have been deleted, so we need to re-query the stack.
					vc.checkout(stackManager.getBranch(name)!!.parent!!.name)
				}

				stackManager.untrackBranch(value)
				vc.delete(name)
			} else {
				stackManager.setHasAskedToDelete(value)
			}
		}

		children.forEach { it.offerBranchDeletion(commandScope, filter) }
	}
}
