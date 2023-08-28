package com.mattprecious.stacker.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.styleCode

internal abstract class StackerCommand(
	name: String? = null,
	private val shortAlias: String? = null,
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
			echo("Stacker must be initialized, first. Please run ${"st repo init".styleCode()}.", err = true)
			throw Abort()
		}
	}

	protected fun requireNoLock(locker: Locker) {
		if (locker.hasLock()) {
			echo(
				message = "A restack is currently in progress. Please run ${"st rebase --abort".styleCode()} or resolve any " +
					"conflicts and run ${"st rebase --continue".styleCode()}.",
				err = true,
			)
			throw Abort()
		}
	}
}
