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

	override suspend fun <T : Operation> beginOperation(
		operation: T,
		block: suspend Locker.LockScope.() -> Unit,
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

	override suspend fun continueOperation(
		block: suspend Locker.LockScope.(operation: Operation) -> Unit,
	) {
		val operation = lockQueries.select().executeAsOne()

		val scope = object : Locker.LockScope {
			override fun updateOperation(operation: Operation) {
				lockQueries.updateOperation(operation)
			}
		}

		with(scope) { block(operation) }

		lockQueries.delete()
	}

	override suspend fun cancelOperation(
		block: suspend (operation: Operation) -> Unit,
	) {
		val operation = lockQueries.select().executeAsOne()
		block(operation)
		lockQueries.delete()
	}
}
