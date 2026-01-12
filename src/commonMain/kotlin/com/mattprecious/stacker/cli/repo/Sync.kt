package com.mattprecious.stacker.cli.repo

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.cli.StackerCliktCommand
import com.mattprecious.stacker.command.repo.repoSync

internal class Sync(private val stacker: StackerDeps) : StackerCliktCommand(shortAlias = "s") {
  private val ask: Boolean by option().flag()

  override val command
    get() = stacker.repoSync(ask)
}
