package com.mattprecious.stacker.cli

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.run
import kotlinx.coroutines.runBlocking

internal abstract class StackerCliktCommand(
  name: String? = null,
  private val shortAlias: String? = null,
) : CliktCommand(name = name) {
  final override fun run() = runBlocking {
    if (command?.run() == false) {
      throw Abort()
    }
  }

  open val command: StackerCommand? = null

  final override fun aliases(): Map<String, List<String>> {
    return buildMap {
      registeredSubcommands().filterIsInstance<StackerCliktCommand>().forEach {
        it.addShortAliases(this, "", emptyList())
      }
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
      registeredSubcommands().filterIsInstance<StackerCliktCommand>().forEach {
        it.addShortAliases(destination, withMyAlias, withMyCommand)
      }
    }
  }
}
