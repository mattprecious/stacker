package com.mattprecious.stacker.cli

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.branch.Branch
import com.mattprecious.stacker.cli.downstack.Downstack
import com.mattprecious.stacker.cli.log.Log
import com.mattprecious.stacker.cli.rebase.Rebase
import com.mattprecious.stacker.cli.repo.Repo
import com.mattprecious.stacker.cli.stack.Stack
import com.mattprecious.stacker.cli.upstack.Upstack

internal class StackerCli(stacker: StackerDeps) : StackerCliktCommand("st") {
  init {
    subcommands(
      Branch(stacker),
      Downstack(stacker),
      Log(stacker),
      Rebase(stacker),
      Repo(stacker),
      Stack(stacker),
      Upstack(stacker),
    )
  }
}
