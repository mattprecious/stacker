package com.mattprecious.stacker.lock

import kotlinx.serialization.Serializable

interface Locker {
	fun hasLock(): Boolean

	fun <T : Operation> beginOperation(
		operation: T,
		block: LockScope<T>.() -> Unit,
	)

	fun continueOperation(
		block: LockScope<Operation>.(operation: Operation) -> Unit,
	)

	fun cancelOperation()
	fun getLockedBranches(): List<BranchState>

	interface LockScope<T : Operation> {
		fun updateOperation(operation: T)
	}

	@Serializable
	sealed interface Operation {
		@Serializable
		data class Restack(
			val branchName: String,
			val ontoName: String,
			val nextBranchToRebase: String,
		) : Operation
	}
}
