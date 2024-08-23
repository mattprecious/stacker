package com.mattprecious.stacker.command

import com.github.ajalt.clikt.core.Abort
import com.github.ajalt.clikt.core.CliktCommand
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.styleCode
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

internal fun Locker.Operation.Restack.perform(
	command: CliktCommand,
	lockScope: Locker.LockScope,
	stackManager: StackManager,
	vc: VersionControl,
	continuing: Boolean = false,
) {
	branches.forEachIndexed { index, branchName ->
		val branch = stackManager.getBranch(branchName)!!
		if (!continuing || index > 0) {
			if (!vc.restack(branchName = branch.name, parentName = branch.parent!!.name, parentSha = branch.parentSha!!)) {
				command.echo(
					message = "Merge conflict. Resolve all conflicts manually and then run " +
						"${"st rebase --continue".styleCode()}. To abort, run ${"st rebase --abort".styleCode()}",
					err = true,
				)
				throw Abort()
			}
		}

		stackManager.updateParentSha(branch.value, vc.getSha(branch.parent!!.name))
		lockScope.updateOperation(copy(branches = branches.subList(index + 1, branches.size)))
	}

	vc.checkout(startingBranch)
}
