package com.mattprecious.stacker.cli.upstack

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.upstack.upstackRestack

internal class Restack(private val stacker: StackerDeps) : StackerCliktCommand(shortAlias = "r") {
  override val command
    get() = stacker.upstackRestack()
}
