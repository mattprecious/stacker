package com.mattprecious.stacker.command.branch

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.branchRename(newName: String): StackerCommand {
  return BranchRename(
    newName = newName,
    configManager = configManager,
    locker = locker,
    stackManager = stackManager,
    vc = vc,
  )
}

internal class BranchRename(
  private val newName: String,
  private val configManager: ConfigManager,
  private val locker: Locker,
  private val stackManager: StackManager,
  private val vc: VersionControl,
) : StackerCommand() {
  override suspend fun StackerCommandScope.work() {
    requireInitialized(configManager)
    requireNoLock(locker)

    val currentBranchName = vc.currentBranchName
    if (
      currentBranchName == configManager.trunk || currentBranchName == configManager.trailingTrunk
    ) {
      printStaticError("Cannot rename a trunk branch.")
      abort()
    }

    val currentBranch = stackManager.getBranch(currentBranchName)
    if (currentBranch != null) {
      stackManager.renameBranch(currentBranch.value, newName)
    }

    vc.renameBranch(branchName = currentBranchName, newName = newName)
  }
}
