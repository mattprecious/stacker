package com.mattprecious.stacker.command.branch

import androidx.compose.runtime.remember
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.collections.TreeNode
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.db.Branch
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.InteractivePrompt
import com.mattprecious.stacker.rendering.PromptState
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.rendering.toAnnotatedString
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl
import kotlinx.collections.immutable.toPersistentList

fun StackerDeps.branchTop(): StackerCommand {
	return BranchTop(
		configManager = configManager,
		locker = locker,
		stackManager = stackManager,
		vc = vc,
	)
}

internal class BranchTop(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireInitialized(configManager)
		requireNoLock(locker)

		val currentBranchName = vc.currentBranchName
		val currentBranch = stackManager.getBranch(currentBranchName)
		if (currentBranch == null) {
			printStaticError(
				buildAnnotatedString {
					append("Branch ")
					this.branch { append(currentBranchName) }
					append(" is not tracked.")
				},
			)
			abort()
		}

		val options = currentBranch.leaves()
		val branch = if (options.size == 1) {
			options.single().name
		} else {
			render { onResult ->
				InteractivePrompt(
					message = "Move up to",
					state = remember {
						PromptState(
							options.toPersistentList(),
							default = null,
							displayTransform = { it.name.toAnnotatedString() },
							valueTransform = { it.name.toAnnotatedString() },
						)
					},
					onSelected = { onResult(it.name) },
				)
			}
		}

		vc.checkout(branch)
	}

	private fun TreeNode<Branch>.leaves(): List<TreeNode<Branch>> {
		return if (children.isEmpty()) {
			listOf(this)
		} else {
			children.flatMap { it.leaves() }
		}
	}
}
