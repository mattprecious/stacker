package com.mattprecious.stacker.command.branch

import androidx.compose.runtime.remember
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.prettyTree
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.InteractivePrompt
import com.mattprecious.stacker.rendering.PromptState
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.rendering.toAnnotatedString
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl
import kotlinx.collections.immutable.toPersistentList

fun StackerDeps.branchTrack(
	branchName: String?,
): StackerCommand {
	return BranchTrack(
		branchName = branchName,
		configManager = configManager,
		locker = locker,
		stackManager = stackManager,
		useFancySymbols = useFancySymbols,
		vc = vc,
	)
}

internal class BranchTrack(
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

		val branchName = branchName ?: vc.currentBranchName
		val currentBranch = stackManager.getBranch(branchName)
		if (currentBranch != null) {
			printStaticError(
				buildAnnotatedString {
					append("Branch ")
					branch { append(branchName) }
					append(" is already tracked.")
				},
			)
			return
		}

		val trunk = configManager.trunk
		val trailingTrunk = configManager.trailingTrunk

		val defaultName = trailingTrunk ?: trunk

		val options = stackManager.getBase()!!.prettyTree(useFancySymbols = useFancySymbols) {
			it.name == trunk || it.name == trailingTrunk || vc.isAncestor(
				branchName = branchName,
				possibleAncestorName = it.name,
			)
		}
		val parent = if (options.size == 1) {
			options.single().branch.name
		} else {
			render { onResult ->
				InteractivePrompt(
					message = buildAnnotatedString {
						append("Choose a parent branch for ")
						branch { append(branchName) }
					},
					state = remember {
						PromptState(
							options.toPersistentList(),
							default = options.find { it.branch.name == defaultName },
							displayTransform = { it.pretty.toAnnotatedString() },
							valueTransform = { it.branch.name.toAnnotatedString() },
						)
					},
					onSelected = { onResult(it.branch.name) },
				)
			}
		}

		val parentSha = vc.getSha(parent)

		stackManager.trackBranch(branchName, parent, parentSha)
	}
}
