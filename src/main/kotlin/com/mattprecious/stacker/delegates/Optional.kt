package com.mattprecious.stacker.delegates

sealed interface Optional<out T : Any?> {
	data class Some<T : Any?>(val value: T) : Optional<T>
	data object None : Optional<Nothing>
}
