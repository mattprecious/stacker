package com.mattprecious.stacker.cli.repo

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Repo(stacker: StackerDeps) : StackerCliktCommand(shortAlias = "r") {
  init {
    subcommands(Init(stacker), Sync(stacker))
  }
}
