package com.mattprecious.stacker

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.YesNoPrompt
import com.mattprecious.stacker.rendering.styleBranch
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.shell.RealShell
import com.mattprecious.stacker.vc.BranchData
import com.mattprecious.stacker.vc.GitVersionControl
import com.mattprecious.stacker.vc.VersionControl
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.buffer
import okio.sink
import okio.source
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.exists

class Stacker : CliktCommand(
	name = "st",
) {
	private val shell = RealShell()
	private val vc = GitVersionControl(shell)
	private val config: Config?

	init {
		val configPath = vc.configDirectory / ".stacker_config"

		config = if (configPath.exists()) {
			val configJson = configPath.source().buffer().use { it.readUtf8() }
			Json.decodeFromString(configJson)
		} else {
			null
		}

		subcommands(
			Init(
				vc = vc,
				configPath = configPath,
				currentConfig = config,
			),
			Branch(
				vc = vc,
				config = config,
			),
		)
	}

	override fun run() {
		if (config == null && currentContext.invokedSubcommand !is Init) {
			error(message = "Stacker must be initialized, first. Please run ${"st init".styleCode()}.")
			throw Abort()
		}
	}
}

class Init(
	private val vc: VersionControl,
	private val configPath: Path,
	private val currentConfig: Config?,
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

		val config = Config(
			trunk = trunk,
			trailingTrunk = trailingTrunk,
		)

		configPath.createParentDirectories()
		configPath.sink().buffer().use { it.writeUtf8(Json.encodeToString(config)) }

		if (currentConfig != null) {
			vc.setMetadata(currentConfig.trunk, null)
			if (currentConfig.trailingTrunk != null) {
				vc.setMetadata(currentConfig.trailingTrunk, null)
			}
		}

		vc.setMetadata(trunk, BranchData(isTrunk = true, parentName = null))
		if (trailingTrunk != null) {
			vc.setMetadata(trailingTrunk, BranchData(isTrunk = true, parentName = null))
		}
	}
}

private class Branch(
	vc: VersionControl,
	config: Config?,
) : CliktCommand() {
	init {
		subcommands(
			Track(vc, config),
			Untrack(vc),
		)
	}

	override fun run() = Unit

	private class Track(
		private val vc: VersionControl,
		private val config: Config?,
	) : CliktCommand() {
		override fun run() {
			if (vc.currentBranch.tracked) {
				error(message = "Branch ${vc.currentBranch.name.styleBranch()} is already tracked.")
				return
			}

			val parent = selectBranch(
				"Select the parent branch for ${vc.currentBranch.name.styleBranch()}",
				default = config!!.trailingTrunk ?: config.trunk,
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
