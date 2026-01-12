package com.mattprecious.stacker.command.branch

import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.perform
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.rendering.code
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.branchRestack(branchName: String?): StackerCommand {
  return BranchRestack(
    branchName = branchName,
    configManager = configManager,
    locker = locker,
    stackManager = stackManager,
    vc = vc,
  )
}

internal class BranchRestack(
  private val branchName: String?,
  private val configManager: ConfigManager,
  private val locker: Locker,
  private val stackManager: StackManager,
  private val vc: VersionControl,
) : StackerCommand() {
  override suspend fun StackerCommandScope.work() {
    requireInitialized(configManager)
    requireNoLock(locker)

    val currentBranchName = vc.currentBranchName
    val branchName = branchName ?: currentBranchName
    if (stackManager.getBranch(currentBranchName) == null) {
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
      Locker.Operation.Restack(startingBranch = currentBranchName, branches = listOf(branchName))

    locker.beginOperation(operation) {
      operation.perform(this@work, this@beginOperation, stackManager, vc)
    }
  }
}
