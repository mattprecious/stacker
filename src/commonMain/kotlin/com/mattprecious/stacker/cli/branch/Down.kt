package com.mattprecious.stacker.cli.branch

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.branch.branchDown

internal class Down(private val stacker: StackerDeps) : StackerCliktCommand(shortAlias = "d") {
  override val command
    get() = stacker.branchDown()
}
