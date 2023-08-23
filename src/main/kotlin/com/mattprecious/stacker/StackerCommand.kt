package com.mattprecious.stacker

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.rendering.styleCode

internal abstract class StackerCommand(
	name: String? = null,
	val shortAlias: String? = null,
) : CliktCommand(
	name = name,
) {
	override fun aliases(): Map<String, List<String>> {
		return buildMap {
			registeredSubcommands()
				.filterIsInstance<StackerCommand>()
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
				"Conflicting aliases! Command ${javaClass.simpleName} tried to add '$withMyAlias', but it already " +
					"points to ${destination[withMyAlias]}."
			}

			destination[withMyAlias] = withMyCommand
			registeredSubcommands()
				.filterIsInstance<StackerCommand>()
				.forEach { it.addShortAliases(destination, withMyAlias, withMyCommand) }
		}
	}

	protected fun requireInitialized(configManager: ConfigManager) {
		if (!configManager.repoInitialized) {
			error("Stacker must be initialized, first. Please run ${"st init".styleCode()}.")
			throw Abort()
		}
	}
}
