package com.mattprecious.stacker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
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
		)
	}

	override fun run() = Unit
}

class Init(
	private val vc: VersionControl,
	private val configPath: Path,
	private val currentConfig: Config?,
) : CliktCommand() {
	override fun run() {
		// TODO: Infer.
		println("Enter the name of your trunk branch, which you open pull requests against. Default is main.")
		val trunk = selectBranch(default = "main")

		println("Do you use a trailing-trunk workflow? Default is No.")
		val useTrailing = readln().ifBlank { "n" }[0].lowercaseChar() == 'y'

		val trailingTrunk = if (!useTrailing) {
			null
		} else {
			println()
			println("Enter the name of your trailing trunk branch, which you branch from.")
			selectBranch()
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

private fun selectBranch(
	default: String? = null,
): String {
	// TODO: Branch picker.
	val input = readln().trim()
	return if (default == null) {
		input.trim().also { require(it.isNotBlank()) }
	} else {
		input.ifBlank { default }
	}
}

fun main(args: Array<String>) = Stacker().main(args)
