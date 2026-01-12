package com.mattprecious.stacker.cli.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.branch.branchRestack

internal class Restack(private val stacker: StackerDeps) : StackerCliktCommand(shortAlias = "r") {
  private val branchName: String? by argument().optional()

  override val command
    get() = stacker.branchRestack(branchName)
}
