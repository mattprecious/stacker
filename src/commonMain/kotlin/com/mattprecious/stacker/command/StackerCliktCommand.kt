package com.mattprecious.stacker.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand

internal abstract class StackerCliktCommand(
	name: String? = null,
	private val shortAlias: String? = null,
) : CliktCommand(
	name = name,
) {
	protected open val command: StackerCommand? = null

	final override fun run() {
		if (command?.run() == false) {
			throw Abort()
		}
	}

	override fun aliases(): Map<String, List<String>> {
		return buildMap {
			registeredSubcommands()
				.filterIsInstance<StackerCliktCommand>()
				.forEach { it.addShortAliases(this, "", emptyList()) }
		}
	}

	private fun addShortAliases(
		destination: MutableMap<String, List<String>>,
		currentPrefix: String,
		currentChain: List<String>,
	) {
		if (shortAlias != null) {
			val withMyAlias = currentPrefix + shortAlias
			val withMyCommand = currentChain + commandName

			check(!destination.contains(withMyAlias)) {
				"Conflicting aliases! Command ${this::class.simpleName} tried to add '$withMyAlias', but it already " +
					"points to ${destination[withMyAlias]}."
			}

			destination[withMyAlias] = withMyCommand
			registeredSubcommands()
				.filterIsInstance<StackerCliktCommand>()
				.forEach { it.addShortAliases(destination, withMyAlias, withMyCommand) }
		}
	}
}
