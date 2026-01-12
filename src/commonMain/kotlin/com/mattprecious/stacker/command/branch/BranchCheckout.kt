package com.mattprecious.stacker.command.branch

import androidx.compose.runtime.remember
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.StackerDeps
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommandScope
import com.mattprecious.stacker.command.name
import com.mattprecious.stacker.command.prettyTree
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.InteractivePrompt
import com.mattprecious.stacker.rendering.PromptState
import com.mattprecious.stacker.rendering.branch
import com.mattprecious.stacker.rendering.toAnnotatedString
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl
import kotlinx.collections.immutable.toPersistentList

fun StackerDeps.branchCheckout(branchName: String?): StackerCommand {
  return BranchCheckout(
    branchName = branchName,
    configManager = configManager,
    locker = locker,
    stackManager = stackManager,
    useFancySymbols = useFancySymbols,
    vc = vc,
  )
}

internal class BranchCheckout(
  private val branchName: String?,
  private val configManager: ConfigManager,
  private val locker: Locker,
  private val stackManager: StackManager,
  private val useFancySymbols: Boolean,
  private val vc: VersionControl,
) : StackerCommand() {
  override suspend fun StackerCommandScope.work() {
    requireInitialized(configManager)
    requireNoLock(locker)

    val branch =
      if (branchName == null) {
        val options = stackManager.getBase()!!.prettyTree(useFancySymbols = useFancySymbols)
        if (options.size == 1) {
          options.single().branch.name
        } else {
          render { onResult ->
            InteractivePrompt(
              message = "Checkout a branch",
              state =
                remember {
                  PromptState(
                    options = options.toPersistentList(),
                    default = options.find { it.branch.name == vc.currentBranchName },
                    displayTransform = { it.pretty.toAnnotatedString() },
                    valueTransform = { it.branch.name.toAnnotatedString() },
                  )
                },
              onSelected = { onResult(it.branch.name) },
            )
          }
        }
      } else if (vc.branches.contains(branchName)) {
        branchName
      } else {
        printStaticError(
          buildAnnotatedString {
            branch { append(branchName) }
            append(" does not match any branches known to git.")
          }
        )

        abort()
      }

    vc.checkout(branch)
  }
}
