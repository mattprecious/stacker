package com.mattprecious.stacker.command

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.jakewharton.mosaic.runMosaicBlocking
import com.mattprecious.stacker.rendering.LocalPrinter
import com.mattprecious.stacker.rendering.Printer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withContext
import platform.posix.exit

internal abstract class StackerMosaicCommand(
	name: String? = null,
	shortAlias: String? = null,
) : StackerCommand(
	name = name,
	shortAlias = shortAlias,
) {
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

		/** `work` has finished and we are tearing down. */
		data object Finished : State
	}

	final override fun run() {
		runMosaicBlocking {
			// Mosaic will wait for all effects to finish before exiting. Finished signals that we should
			// terminate this collect in order to tear down.
			val currentState = remember { state.takeWhile { it !is State.Finished } }
				.collectAsState(state.value)
				.value

			printer.Messages()

			LaunchedEffect(Unit) {
				// Don't block the render thread.
				withContext(Dispatchers.IO) {
					StackerCommandScope().work()
					state.value = State.Finished

					// Mosaic renders every 50ms, which means that our 'graceful' finish mechanism takes 100ms
					// to fully finish. This is noticeable in this project (`st ls` only takes ~25ms in small
					// repos), so we'll short-circuit with a harsh exit for now.
					// https://github.com/JakeWharton/mosaic/issues/507
					exit(0)
				}
			}

			if (currentState is State.Rendering<*>) {
				CompositionLocalProvider(LocalPrinter provides printer) {
					currentState.content { state.value = State.DeliveringRenderResult(it) }
				}
			}
		}
	}

	abstract suspend fun StackerCommandScope.work()

	inner class StackerCommandScope {
		fun printStatic(message: String) {
			printer.printStatic(message)
		}

		suspend fun <R> render(content: @Composable (onResult: (R) -> Unit) -> Unit): R {
			state.value = State.Rendering(content)
			return state.filterIsInstance<State.DeliveringRenderResult<R>>().first().result.also {
				state.value = State.Working
			}
		}
	}
}
