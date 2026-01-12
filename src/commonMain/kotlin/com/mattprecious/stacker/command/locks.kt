package com.mattprecious.stacker.command

import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.code
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal suspend fun Locker.Operation.Restack.perform(
  commandScope: StackerCommandScope,
  lockScope: Locker.LockScope,
  stackManager: StackManager,
  vc: VersionControl,
  continuing: Boolean = false,
) {
  branches.forEachIndexed { index, branchName ->
    val branch = stackManager.getBranch(branchName)!!
    if (!continuing || index > 0) {
      if (
        !vc.restack(
          branchName = branch.name,
          parentName = branch.parent!!.name,
          parentSha = branch.parentSha!!,
        )
      ) {
        commandScope.printStaticError(
          buildAnnotatedString {
            append("Merge conflict. Resolve all conflicts manually and then run ")
            code { append("st rebase --continue") }
            append(". To abort, run ")
            code { append("st rebase --abort") }
            append(".")
          }
        )
        commandScope.abort()
      }
    }

    stackManager.updateParentSha(branch.value, vc.getSha(branch.parent!!.name))
    lockScope.updateOperation(copy(branches = branches.subList(index + 1, branches.size)))
  }

  vc.checkout(startingBranch)
}
