package com.mattprecious.stacker.command.branch

import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.branchDown(): StackerCommand {
  return BranchDown(
    configManager = configManager,
    locker = locker,
    stackManager = stackManager,
    vc = vc,
  )
}

internal class BranchDown(
  private val configManager: ConfigManager,
  private val locker: Locker,
  private val stackManager: StackManager,
  private val vc: VersionControl,
) : StackerCommand() {
  override suspend fun StackerCommandScope.work() {
    requireInitialized(configManager)
    requireNoLock(locker)

    val branchName = vc.currentBranchName
    val branch = stackManager.getBranch(branchName)
    if (branch == null) {
      printStaticError(
        buildAnnotatedString {
          append("Branch ")
          this.branch { append(branchName) }
          append(" is not tracked.")
        }
      )
      abort()
    }

    val parent = branch.parent
    if (parent != null) {
      vc.checkout(parent.name)
    }
  }
}
