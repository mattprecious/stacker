package com.mattprecious.stacker

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.shell.RealShell
import com.mattprecious.stacker.vc.GitVersionControl
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
			Init(configPath),
		)
	}

	override fun run() = Unit
}

class Init(
	private val configPath: Path,
) : CliktCommand() {
	override fun run() {
		// TODO: Infer.
		println("Enter the name of your trunk branch, which you open pull requests against. Default is main.")

		// TODO: Branch picker.
		val trunk = readln().ifBlank { "main" }

		println("Do you use a trailing-trunk workflow? Default is No.")
		val useTrailing = readln().ifBlank { "n" }[0].lowercaseChar() == 'y'

		val trailingTrunk = if (!useTrailing) {
			null
		} else {
			println()
			println("Enter the name of your trailing trunk branch, which you branch from.")

			// TODO: Branch picker.
			readln().trim().also { require(it.isNotBlank()) }
		}

		val config = Config(
			trunk = trunk,
			trailingTrunk = trailingTrunk,
		)

		configPath.createParentDirectories()
		configPath.sink().buffer().use { it.writeUtf8(Json.encodeToString(config)) }
	}
}

fun main(args: Array<String>) = Stacker().main(args)
