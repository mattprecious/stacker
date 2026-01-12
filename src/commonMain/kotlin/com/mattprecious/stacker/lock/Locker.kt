package com.mattprecious.stacker.lock

import kotlinx.serialization.Serializable

interface Locker {
  fun hasLock(): Boolean

  suspend fun <T : Operation> beginOperation(operation: T, block: suspend LockScope.() -> Unit)

  suspend fun continueOperation(block: suspend LockScope.(operation: Operation) -> Unit)

  suspend fun cancelOperation(block: suspend (operation: Operation) -> Unit)

  interface LockScope {
    fun updateOperation(operation: Operation)
  }

  @Serializable
  sealed interface Operation {
    @Serializable
    data class Restack(val startingBranch: String, val branches: List<String>) : Operation
  }
}
