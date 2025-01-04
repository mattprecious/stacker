package com.mattprecious.stacker

import com.mattprecious.stacker.command.CommandExecutor
import com.mattprecious.stacker.command.branch.BranchBottom
import com.mattprecious.stacker.command.branch.BranchCheckout
import com.mattprecious.stacker.command.branch.BranchCreate
import com.mattprecious.stacker.command.branch.BranchDelete
import com.mattprecious.stacker.command.branch.BranchDown
import com.mattprecious.stacker.command.branch.BranchRename
import com.mattprecious.stacker.command.branch.BranchRestack
import com.mattprecious.stacker.command.branch.BranchSubmit
import com.mattprecious.stacker.command.branch.BranchTop
import com.mattprecious.stacker.command.branch.BranchTrack
import com.mattprecious.stacker.command.branch.BranchUntrack
import com.mattprecious.stacker.command.branch.BranchUp
import com.mattprecious.stacker.command.downstack.DownstackEdit
import com.mattprecious.stacker.command.log.LogShort
import com.mattprecious.stacker.command.rebase.RebaseAbort
import com.mattprecious.stacker.command.rebase.RebaseContinue
import com.mattprecious.stacker.command.repo.RepoInit
import com.mattprecious.stacker.command.repo.RepoSync
import com.mattprecious.stacker.command.stack.StackSubmit
import com.mattprecious.stacker.command.upstack.UpstackOnto
import com.mattprecious.stacker.command.upstack.UpstackRestack
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

class Stacker(
	private val commandExecutor: CommandExecutor,
	private val configManager: ConfigManager,
	private val locker: Locker,
	private val remote: Remote,
	private val stackManager: StackManager,
	private val useFancySymbols: Boolean,
	private val vc: VersionControl,
) {
	suspend fun cleanUpBranches() {
		stackManager.untrackBranches(vc.checkBranches(stackManager.trackedBranchNames.toSet()))
	}

	suspend fun branchBottom(): Boolean {
		return commandExecutor.execute(BranchBottom(configManager, locker, stackManager, vc))
	}

	suspend fun branchCheckout(branchName: String?): Boolean {
		return commandExecutor.execute(
			BranchCheckout(branchName, configManager, locker, stackManager, useFancySymbols, vc),
		)
	}

	suspend fun branchCreate(branchName: String): Boolean {
		return commandExecutor.execute(
			BranchCreate(branchName, configManager, locker, stackManager, vc),
		)
	}

	suspend fun branchDelete(branchName: String?): Boolean {
		return commandExecutor.execute(
			BranchDelete(branchName, configManager, locker, stackManager, vc),
		)
	}

	suspend fun branchDown(): Boolean {
		return commandExecutor.execute(BranchDown(configManager, locker, stackManager, vc))
	}

	suspend fun branchRename(newName: String): Boolean {
		return commandExecutor.execute(BranchRename(newName, configManager, locker, stackManager, vc))
	}

	suspend fun branchRestack(branchName: String?): Boolean {
		return commandExecutor.execute(
			BranchRestack(branchName, configManager, locker, stackManager, vc),
		)
	}

	suspend fun branchSubmit(): Boolean {
		return commandExecutor.execute(BranchSubmit(configManager, locker, remote, stackManager, vc))
	}

	suspend fun branchTop(): Boolean {
		return commandExecutor.execute(BranchTop(configManager, locker, stackManager, vc))
	}

	suspend fun branchTrack(branchName: String?): Boolean {
		return commandExecutor.execute(
			BranchTrack(branchName, configManager, locker, stackManager, useFancySymbols, vc),
		)
	}

	suspend fun branchUntrack(branchName: String?): Boolean {
		return commandExecutor.execute(
			BranchUntrack(branchName, configManager, locker, stackManager, vc),
		)
	}

	suspend fun branchUp(): Boolean {
		return commandExecutor.execute(BranchUp(configManager, locker, stackManager, vc))
	}

	suspend fun downstackEdit(): Boolean {
		return commandExecutor.execute(DownstackEdit(configManager, locker, stackManager, vc))
	}

	suspend fun logShort(): Boolean {
		return commandExecutor.execute(LogShort(configManager, stackManager, useFancySymbols, vc))
	}

	suspend fun rebaseAbort(): Boolean {
		return commandExecutor.execute(RebaseAbort(configManager, locker, vc))
	}

	suspend fun rebaseContinue(): Boolean {
		return commandExecutor.execute(RebaseContinue(configManager, locker, stackManager, vc))
	}

	suspend fun repoInit(): Boolean {
		return commandExecutor.execute(RepoInit(configManager, locker, vc))
	}

	suspend fun repoSync(): Boolean {
		return commandExecutor.execute(RepoSync(configManager, remote, stackManager, vc))
	}

	suspend fun stackSubmit(): Boolean {
		return commandExecutor.execute(StackSubmit(configManager, locker, remote, stackManager, vc))
	}

	suspend fun upstackOnto(): Boolean {
		return commandExecutor.execute(
			UpstackOnto(configManager, locker, stackManager, useFancySymbols, vc),
		)
	}

	suspend fun upstackRestack(): Boolean {
		return commandExecutor.execute(UpstackRestack(configManager, locker, stackManager, vc))
	}
}
