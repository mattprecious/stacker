package com.mattprecious.stacker.cli.branch

import com.github.ajalt.clikt.core.subcommands
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand

internal class Branch(stacker: StackerDeps) : StackerCliktCommand(shortAlias = "b") {
  init {
    subcommands(
      Bottom(stacker),
      Checkout(stacker),
      Create(stacker),
      Delete(stacker),
      Down(stacker),
      Rename(stacker),
      Restack(stacker),
      Submit(stacker),
      Top(stacker),
      Track(stacker),
      Untrack(stacker),
      Up(stacker),
    )
  }
}
