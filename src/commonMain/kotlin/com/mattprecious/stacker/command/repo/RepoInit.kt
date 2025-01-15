package com.mattprecious.stacker.command.repo

import androidx.compose.runtime.remember
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.InteractivePrompt
import com.mattprecious.stacker.rendering.PromptState
import com.mattprecious.stacker.rendering.YesNoPrompt
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.repoInit(): StackerCommand {
	return RepoInit(
		configManager = configManager,
		locker = locker,
		vc = vc,
	)
}

internal class RepoInit(
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val vc: VersionControl,
) : StackerCommand() {
	override suspend fun StackerCommandScope.work() {
		requireNoLock(locker)

		val (currentTrunk, currentTrailingTrunk) = if (configManager.repoInitialized) {
			configManager.trunk to configManager.trailingTrunk
		} else {
			null to null
		}

		val branches = vc.branches
		if (branches.isEmpty()) {
			// We need a SHA in order to initialize. Additionally, I don't know how to get the current
			// branch name when it's an unborn branch.
			printStaticError(
				"Stacker cannot be initialized in a completely empty repository. Please make a commit " +
					"first.",
			)
			abort()
		}

		val defaultTrunk = run defaultTrunk@{
			if (currentTrunk != null) return@defaultTrunk currentTrunk

			if (branches.size == 1) return@defaultTrunk null

			val defaultBranch = vc.defaultBranch
			if (defaultBranch != null && branches.contains(defaultBranch)) {
				return@defaultTrunk defaultBranch
			}

			return@defaultTrunk vc.currentBranchName
		}

		val trunk = render { onResult ->
			InteractivePrompt(
				message = "Select your trunk branch, which you open pull requests against",
				state = remember {
					PromptState(
						options = branches,
						default = defaultTrunk,
						displayTransform = { it },
						valueTransform = { it },
					)
				},
				onSelected = { onResult(it) },
			)
		}

		val trunkSha = vc.getSha(trunk)

		val useTrailing = render { onResult ->
			YesNoPrompt(
				message = "Do you use a trailing-trunk workflow?",
				default = currentTrailingTrunk != null,
				onSubmit = { onResult(it) },
			)
		}

		val trailingTrunk: String? = if (!useTrailing) {
			null
		} else {
			// TODO: This assumes that the trailing trunk branch already exists. It will fail if there's
			//  only one branch in the repo.
			render { onResult ->
				InteractivePrompt(
					message = "Select your trailing trunk branch, which you branch from",
					state = remember {
						PromptState(
							options = branches.filterNot { it == trunk },
							default = currentTrailingTrunk,
							displayTransform = { it },
							valueTransform = { it },
						)
					},
					onSelected = { onResult(it) },
				)
			}
		}

		configManager.initializeRepo(trunk = trunk, trunkSha = trunkSha, trailingTrunk = trailingTrunk)
	}
}
