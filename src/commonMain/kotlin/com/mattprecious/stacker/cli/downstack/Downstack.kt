package com.mattprecious.stacker.cli.downstack

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Downstack(stacker: StackerDeps) : StackerCliktCommand(shortAlias = "ds") {
  init {
    subcommands(Edit(stacker))
  }
}
