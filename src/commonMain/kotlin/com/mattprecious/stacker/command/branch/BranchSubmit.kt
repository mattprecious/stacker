package com.mattprecious.stacker.command.branch

import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.requireAuthenticated
import com.mattprecious.stacker.command.submit
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.rendering.code
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.branchSubmit(): StackerCommand {
  return BranchSubmit(
    configManager = configManager,
    locker = locker,
    remote = remote,
    stackManager = stackManager,
    vc = vc,
  )
}

internal class BranchSubmit(
  private val configManager: ConfigManager,
  private val locker: Locker,
  private val remote: Remote,
  private val stackManager: StackManager,
  private val vc: VersionControl,
) : StackerCommand() {
  override suspend fun StackerCommandScope.work() {
    requireInitialized(configManager)
    requireNoLock(locker)

    val currentBranch = stackManager.getBranch(vc.currentBranchName)
    if (currentBranch == null) {
      printStaticError(
        buildAnnotatedString {
          append("Cannot create a pull request from ")
          branch { append(vc.currentBranchName) }
          append(" since it is not tracked. Please track with ")
          code { append("st branch track") }
          append(".")
        }
      )
      abort()
    }

    if (
      currentBranch.name == configManager.trunk || currentBranch.name == configManager.trailingTrunk
    ) {
      printStaticError(
        buildAnnotatedString {
          append("Cannot create a pull request from trunk branch ")
          branch { append(currentBranch.name) }
          append(".")
        }
      )
      abort()
    }

    requireAuthenticated(remote)

    vc.pushBranches(listOf(currentBranch.name))
    currentBranch.submit(this@work, configManager, remote, stackManager, vc)
  }
}
