package com.mattprecious.stacker.cli.log

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Log(stacker: StackerDeps) : StackerCliktCommand(shortAlias = "l") {
  init {
    subcommands(Short(stacker))
  }
}
