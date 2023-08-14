package com.mattprecious.stacker.lock

import com.mattprecious.stacker.db.RepoDatabase
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

class RealLocker(
	db: RepoDatabase,
	private val stackManager: StackManager,
	private val vc: VersionControl,
) : Locker {
	private val lockQueries = db.lockQueries

	override fun hasLock(): Boolean {
		return lockQueries.hasLock().executeAsOne()
	}

	override fun <T : Locker.Operation> beginOperation(
		operation: T,
		block: Locker.LockScope<T>.() -> Unit,
	) {
		require(!hasLock())

		val branchStates = vc.getShas(stackManager.trackedBranchNames).map {
			BranchState(it.name, it.sha)
		}

		lockQueries.lock(
			branches = branchStates,
			operation = operation,
		)

		val scope = object : Locker.LockScope<T> {
			override fun updateOperation(operation: T) {
				lockQueries.updateOperation(operation)
			}
		}

		with(scope) { block() }

		lockQueries.delete()
	}

	override fun continueOperation(block: Locker.LockScope<Locker.Operation>.(operation: Locker.Operation) -> Unit) {
		val lock = lockQueries.select().executeAsOne()

		val scope = object : Locker.LockScope<Locker.Operation> {
			override fun updateOperation(operation: Locker.Operation) {
				lockQueries.updateOperation(operation)
			}
		}

		with(scope) { block(lock.operation) }

		lockQueries.delete()
	}

	override fun cancelOperation() {
		lockQueries.delete()
	}

	override fun getLockedBranches(): List<BranchState> {
		return lockQueries.select().executeAsOne().branches
	}
}
