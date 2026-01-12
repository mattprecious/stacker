package com.mattprecious.stacker.command.rebase

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.rebaseAbort(): StackerCommand {
  return RebaseAbort(configManager = configManager, locker = locker, vc = vc)
}

internal class RebaseAbort(
  private val configManager: ConfigManager,
  private val locker: Locker,
  private val vc: VersionControl,
) : StackerCommand() {
  override suspend fun StackerCommandScope.work() {
    requireInitialized(configManager)

    if (!locker.hasLock()) {
      printStaticError("Nothing to abort.")
      abort()
    }

    locker.cancelOperation { operation ->
      when (operation) {
        is Locker.Operation.Restack -> {
          vc.abortRebase()
          vc.checkout(operation.startingBranch)
        }
      }
    }
  }
}
