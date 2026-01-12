package com.mattprecious.stacker.cli.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.branch.branchRename

internal class Rename(private val stacker: StackerDeps) : StackerCliktCommand(shortAlias = "rn") {
  private val newName by argument()

  override val command
    get() = stacker.branchRename(newName)
}
