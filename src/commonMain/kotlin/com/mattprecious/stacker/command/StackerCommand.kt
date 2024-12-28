package com.mattprecious.stacker.command

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.jakewharton.mosaic.runMosaicBlocking
import com.jakewharton.mosaic.text.AnnotatedString
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.rendering.LocalPrinter
import com.mattprecious.stacker.rendering.Printer
import com.mattprecious.stacker.rendering.code
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withContext

internal abstract class StackerCommand {
	private val printer = Printer()
	private val state = MutableStateFlow<State>(State.Working)

	private sealed interface State {
		/** [work] is executing and we are not rendering any dynamic content. */
		data object Working : State

		/**
		 * `work` has provided dynamic content to render.
		 *
		 * @param content A callback to invoke when rendering should be finished.
		 */
		data class Rendering<R>(
			val content: @Composable (onResult: (R) -> Unit) -> Unit,
		) : State

		/** The [Rendering] state has returned a `result`. We will deliver it back to [work]. */
		data class DeliveringRenderResult<R>(val result: R) : State

		sealed interface TerminalState : State

		/** `work` has finished and we are tearing down. */
		data object Finished : TerminalState

		/** `work` has aborted and we are tearing down. */
		data object Aborted : TerminalState
	}

	fun run(): Boolean {
		runMosaicBlocking {
			// Mosaic will wait for all effects to finish before exiting. Finished and Aborted signal that
			// we should terminate this collect in order to tear down.
			val currentState = remember {
				state.transformWhile {
					// We need to emit the terminal state so that the LaunchedEffect below can be interrupted.
					emit(it)
					it !is State.TerminalState
				}
			}
				.collectAsState(state.value)
				.value

			printer.Messages()

			if (currentState !is State.TerminalState) {
				LaunchedEffect(Unit) {
					// Don't block the render thread.
					withContext(Dispatchers.IO) {
						StackerCommandScope().work()
						state.value = State.Finished
					}
				}
			}

			if (currentState is State.Rendering<*>) {
				CompositionLocalProvider(LocalPrinter provides printer) {
					currentState.content { state.value = State.DeliveringRenderResult(it) }
				}
			}
		}

		return state.value is State.Finished
	}

	protected open suspend fun StackerCommandScope.work() {}

	inner class StackerCommandScope {
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
			state.value = State.Rendering(content)
			return state.filterIsInstance<State.DeliveringRenderResult<R>>().first().result.also {
				state.value = State.Working
			}
		}

		suspend fun requireInitialized(configManager: ConfigManager) {
			if (!configManager.repoInitialized) {
				printStaticError(
					buildAnnotatedString {
						append("Stacker must be initialized, first. Please run ")
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
			require(state.value !is State.TerminalState)
			state.value = State.Aborted
			awaitCancellation()
		}
	}
}
