package com.mattprecious.stacker

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.config.RealConfigManager
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.shell.RealShell
import com.mattprecious.stacker.vc.BranchData
import com.mattprecious.stacker.vc.GitVersionControl
import com.mattprecious.stacker.vc.VersionControl

class Stacker : CliktCommand(
	name = "st",
) {
	private val shell = RealShell()
	private val vc = GitVersionControl(shell)
	private val configManager = RealConfigManager(vc)

	init {
		subcommands(
			Init(
				configManager = configManager,
			),
			Branch(
				vc = vc,
				configManager = configManager,
			),
		)
	}

	override fun run() {
		if (!configManager.repoInitialized && currentContext.invokedSubcommand !is Init) {
			error(message = "Stacker must be initialized, first. Please run ${"st init".styleCode()}.")
			throw Abort()
		}
	}
}

class Init(
	private val configManager: ConfigManager,
) : CliktCommand() {
	override fun run() {
		// TODO: Infer.
		val trunk = selectBranch(
			text = "Enter the name of your trunk branch, which you open pull requests against",
			default = "main",
		)

		val useTrailing = YesNoPrompt(
			terminal = currentContext.terminal,
			prompt = "Do you use a trailing-trunk workflow?",
			default = false,
		).ask() == true

		val trailingTrunk = if (!useTrailing) {
			null
		} else {
			selectBranch("Enter the name of your trailing trunk branch, which you branch from")
		}

		configManager.initializeRepo(trunk = trunk, trailingTrunk = trailingTrunk)
	}
}

private class Branch(
	vc: VersionControl,
	configManager: ConfigManager,
) : CliktCommand() {
	init {
		subcommands(
			Track(vc, configManager),
			Untrack(vc),
			Create(vc),
		)
	}

	override fun run() = Unit

	private class Track(
		private val vc: VersionControl,
		private val configManager: ConfigManager,
	) : CliktCommand() {
		override fun run() {
			if (vc.currentBranch.tracked) {
				error(message = "Branch ${vc.currentBranch.name.styleBranch()} is already tracked.")
				return
			}

			val parent = selectBranch(
				"Select the parent branch for ${vc.currentBranch.name.styleBranch()}",
				default = configManager.trailingTrunk ?: configManager.trunk,
			)

			vc.setMetadata(vc.currentBranch.name, BranchData(isTrunk = false, parentName = parent))
		}
	}

	private class Untrack(
		private val vc: VersionControl,
	) : CliktCommand() {
		override fun run() {
			if (!vc.currentBranch.tracked) {
				error(message = "Branch ${vc.currentBranch.name.styleBranch()} is already not tracked.")
				return
			}

			vc.setMetadata(vc.currentBranch.name, null)
		}
	}

	private class Create(
		private val vc: VersionControl,
	) : CliktCommand() {
		private val branchName by argument()

		override fun run() {
			if (!vc.currentBranch.tracked) {
				error(
					message = "Cannot branch from ${vc.currentBranch.name.styleBranch()} since it is not tracked. " +
						"Please track with ${"st branch track".styleCode()}.",
				)
				throw Abort()
			}

			vc.createBranchFromCurrent(branchName)
		}
	}
}

context(CliktCommand)
private fun error(message: String) {
	echo(message, err = true)
}

context(CliktCommand)
private fun selectBranch(
	text: String,
	default: String? = null,
): String {
	// TODO: Branch picker.
	return prompt(
		text = text,
		default = default,
	) {
		when (it.isBlank()) {
			false -> ConversionResult.Valid(it)
			true -> ConversionResult.Invalid("Cannot be blank.")
		}
	}!!
}

fun main(args: Array<String>) = Stacker().main(args)
