package com.mattprecious.stacker.command.upstack

import androidx.compose.runtime.remember
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.collections.all
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.perform
import com.mattprecious.stacker.command.prettyTree
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.InteractivePrompt
import com.mattprecious.stacker.rendering.PromptState
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.rendering.code
import com.mattprecious.stacker.rendering.toAnnotatedString
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl
import kotlinx.collections.immutable.toPersistentList

fun StackerDeps.upstackOnto(): StackerCommand {
  return UpstackOnto(
    configManager = configManager,
    locker = locker,
    stackManager = stackManager,
    useFancySymbols = useFancySymbols,
    vc = vc,
  )
}

internal class UpstackOnto(
  private val configManager: ConfigManager,
  private val locker: Locker,
  private val stackManager: StackManager,
  private val useFancySymbols: Boolean,
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
          append("Cannot retarget ")
          branch { append(currentBranchName) }
          append(" since it is not tracked. Please track with ")
          code { append("st branch track") }
          append(".")
        }
      )
      abort()
    }

    if (
      currentBranchName == configManager.trunk || currentBranchName == configManager.trailingTrunk
    ) {
      printStaticError("Cannot retarget a trunk branch.")
      abort()
    }

    val options =
      stackManager.getBase()!!.prettyTree(useFancySymbols = useFancySymbols) {
        it.name != currentBranchName
      }

    val newParent = render { onResult ->
      InteractivePrompt(
        message =
          buildAnnotatedString {
            append("Select the parent branch for ")
            branch { append(currentBranchName) }
          },
        state =
          remember {
            PromptState(
              options.toPersistentList(),
              default = options.find { it.branch.name == currentBranch.parent!!.name },
              displayTransform = { it.pretty.toAnnotatedString() },
              valueTransform = { it.branch.name.toAnnotatedString() },
            )
          },
        onSelected = { onResult(it.branch) },
      )
    }

    stackManager.updateParent(currentBranch.value, newParent.value)

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
