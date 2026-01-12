package com.mattprecious.stacker.command.upstack

import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.collections.all
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.perform
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.rendering.code
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.upstackRestack(): StackerCommand {
  return UpstackRestack(
    configManager = configManager,
    locker = locker,
    stackManager = stackManager,
    vc = vc,
  )
}

internal class UpstackRestack(
  private val configManager: ConfigManager,
  private val locker: Locker,
  private val stackManager: StackManager,
  private val vc: VersionControl,
) : StackerCommand() {
  override suspend fun StackerCommandScope.work() {
    requireInitialized(configManager)
    requireNoLock(locker)

    val currentBranchName = vc.currentBranchName
    val currentBranch = stackManager.getBranch(currentBranchName)
    if (currentBranch == null) {
      printStaticError(
        buildAnnotatedString {
          append("Cannot restack ")
          branch { append(currentBranchName) }
          append(" since it is not tracked. Please track with ")
          code { append("st branch track") }
          append(".")
        }
      )
      abort()
    }

    val operation =
      Locker.Operation.Restack(
        startingBranch = currentBranch.name,
        currentBranch.all.map { it.name }.toList(),
      )

    locker.beginOperation(operation) {
      operation.perform(this@work, this@beginOperation, stackManager, vc)
    }
  }
}
