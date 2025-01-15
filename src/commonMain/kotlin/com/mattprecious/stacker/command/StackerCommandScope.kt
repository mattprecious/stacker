package com.mattprecious.stacker.command

import androidx.compose.runtime.Composable
import com.jakewharton.mosaic.text.AnnotatedString
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.Printer
import com.mattprecious.stacker.rendering.code
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

class StackerCommandScope internal constructor(
	private val printer: Printer,
	private val state: MutableStateFlow<StackerCommand.State>,
) {
	fun printStatic(message: String) {
		printer.printStatic(message)
	}

	fun printStatic(message: AnnotatedString) {
		printer.printStatic(message)
	}

	fun printStaticError(message: String) {
		// TODO: Red.
		printer.printStatic(message)
	}

	fun printStaticError(message: AnnotatedString) {
		// TODO: Red.
		printer.printStatic(message)
	}

	suspend fun <R> render(content: @Composable (onResult: (R) -> Unit) -> Unit): R {
		state.value = StackerCommand.State.Rendering(content)
		return state.filterIsInstance<StackerCommand.State.DeliveringRenderResult<R>>().first().result.also {
			state.value = StackerCommand.State.Working
		}
	}

	suspend fun requireInitialized(configManager: ConfigManager) {
		if (!configManager.repoInitialized) {
			printStaticError(
				buildAnnotatedString {
					append("Stacker must be initialized first. Please run ")
					code { append("st repo init") }
					append(".")
				},
			)
			abort()
		}
	}

	suspend fun requireNoLock(locker: Locker) {
		if (locker.hasLock()) {
			printStaticError(
				buildAnnotatedString {
					append("A restack is currently in progress. Please run ")
					code { append("st rebase --abort") }
					append(" or resolve any conflicts and run ")
					code { append("st rebase --continue") }
					append(".")
				},
			)
			abort()
		}
	}

	suspend fun abort(): Nothing {
		require(state.value !is StackerCommand.State.TerminalState)
		state.value = StackerCommand.State.Aborted
		awaitCancellation()
	}
}
