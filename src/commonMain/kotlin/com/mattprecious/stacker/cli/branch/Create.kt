package com.mattprecious.stacker.cli.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.branch.branchCreate

internal class Create(private val stacker: StackerDeps) : StackerCliktCommand(shortAlias = "c") {
  private val branchName by argument()

  override val command
    get() = stacker.branchCreate(branchName)
}
