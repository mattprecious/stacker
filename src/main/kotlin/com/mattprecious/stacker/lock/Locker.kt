package com.mattprecious.stacker.lock

import kotlinx.serialization.Serializable

interface Locker {
	fun hasLock(): Boolean

	fun <T : Operation> beginOperation(
		operation: T,
		block: LockScope.() -> Unit,
	)

	fun continueOperation(
		block: LockScope.(operation: Operation) -> Unit,
	)

	fun cancelOperation()

	interface LockScope {
		fun updateOperation(operation: Operation)
	}

	@Serializable
	sealed interface Operation {
		@Serializable
		data class Restack(
			val startingBranch: String,
			val branches: List<String>,
		) : Operation
	}
}
