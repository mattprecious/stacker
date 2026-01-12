package com.mattprecious.stacker.cli.branch

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.branch.branchTrack

internal class Track(private val stacker: StackerDeps) : StackerCliktCommand(shortAlias = "tr") {
  private val branchName: String? by argument().optional()

  override val command
    get() = stacker.branchTrack(branchName)
}
