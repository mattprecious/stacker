package com.mattprecious.stacker.command.rebase

import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.perform
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

fun StackerDeps.rebaseContinue(): StackerCommand {
  return RebaseContinue(
    configManager = configManager,
    locker = locker,
    stackManager = stackManager,
    vc = vc,
  )
}

internal class RebaseContinue(
  private val configManager: ConfigManager,
  private val locker: Locker,
  private val stackManager: StackManager,
  private val vc: VersionControl,
) : StackerCommand() {
  override suspend fun StackerCommandScope.work() {
    requireInitialized(configManager)

    if (!locker.hasLock()) {
      printStaticError("Nothing to continue.")
      abort()
    }

    locker.continueOperation { operation ->
      when (operation) {
        is Locker.Operation.Restack -> {
          if (vc.continueRebase(operation.branches.first())) {
            operation.perform(this@work, this, stackManager, vc, continuing = true)
          } else {
            printStaticError("Unresolved merge conflicts.")
            abort()
          }
        }
      }
    }
  }
}
