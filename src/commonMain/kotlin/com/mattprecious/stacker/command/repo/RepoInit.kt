package com.mattprecious.stacker.command.repo

import androidx.compose.runtime.remember
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.delegates.Optional
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.InteractivePrompt
import com.mattprecious.stacker.rendering.PromptState
import com.mattprecious.stacker.rendering.YesNoPrompt
import com.mattprecious.stacker.rendering.toAnnotatedString
import com.mattprecious.stacker.vc.VersionControl
import kotlinx.collections.immutable.toPersistentList

fun StackerDeps.repoInit(
	trunk: String? = null,
	trailingTrunk: Optional<String>? = null,
): StackerCommand {
	return RepoInit(
		trunk = trunk,
		trailingTrunk = trailingTrunk,
		configManager = configManager,
		locker = locker,
		vc = vc,
	)
}

internal class RepoInit(
	private val trunk: String?,
	private val trailingTrunk: Optional<String>?,
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

		val trunk = trunk ?: render { onResult ->
			InteractivePrompt(
				message = "Select your trunk branch, which you open pull requests against",
				state = remember {
					PromptState(
						branches.toPersistentList(),
						default = defaultTrunk,
						displayTransform = { it.toAnnotatedString() },
						valueTransform = { it.toAnnotatedString() },
					)
				},
				onSelected = { onResult(it) },
			)
		}

		val trunkSha = vc.getSha(trunk)

		val useTrailing = if (trailingTrunk == null) {
			if (branches.size == 1) {
				false
			} else {
				render { onResult ->
					YesNoPrompt(
						message = "Do you use a trailing-trunk workflow?",
						default = currentTrailingTrunk != null,
						onSubmit = { onResult(it) },
					)
				}
			}
		} else {
			trailingTrunk is Optional.Some
		}

		val trailingTrunk: String? = if (!useTrailing) {
			null
		} else {
			(trailingTrunk as? Optional.Some)?.value ?: render { onResult ->
				InteractivePrompt(
					message = "Select your trailing trunk branch, which you branch from",
					state = remember {
						PromptState(
							branches.filterNot { it == trunk }.toPersistentList(),
							default = currentTrailingTrunk,
							displayTransform = { it.toAnnotatedString() },
							valueTransform = { it.toAnnotatedString() },
						)
					},
					onSelected = { onResult(it) },
				)
			}
		}

		configManager.initializeRepo(
			scope = this,
			trunk = trunk,
			trunkSha = trunkSha,
			trailingTrunk = trailingTrunk,
		)
	}
}
