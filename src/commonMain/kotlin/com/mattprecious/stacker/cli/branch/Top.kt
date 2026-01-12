package com.mattprecious.stacker.cli.branch

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.branch.branchTop

internal class Top(private val stacker: StackerDeps) : StackerCliktCommand(shortAlias = "t") {
  override val command
    get() = stacker.branchTop()
}
