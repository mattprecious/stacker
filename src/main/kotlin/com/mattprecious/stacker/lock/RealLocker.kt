package com.mattprecious.stacker.lock

import com.mattprecious.stacker.db.RepoDatabase
import com.mattprecious.stacker.lock.Locker.Operation
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

	override fun <T : Operation> beginOperation(
		operation: T,
		block: Locker.LockScope.() -> Unit,
	) {
		require(!hasLock())

		lockQueries.lock(operation = operation)

		val scope = object : Locker.LockScope {
			override fun updateOperation(operation: Operation) {
				lockQueries.updateOperation(operation)
			}
		}

		with(scope) { block() }

		lockQueries.delete()
	}

	override fun continueOperation(block: Locker.LockScope.(operation: Operation) -> Unit) {
		val operation = lockQueries.select().executeAsOne()

		val scope = object : Locker.LockScope {
			override fun updateOperation(operation: Operation) {
				lockQueries.updateOperation(operation)
			}
		}

		with(scope) { block(operation) }

		lockQueries.delete()
	}

	override fun cancelOperation(
		block: (operation: Operation) -> Unit,
	) {
		val operation = lockQueries.select().executeAsOne()
		block(operation)
		lockQueries.delete()
	}
}
