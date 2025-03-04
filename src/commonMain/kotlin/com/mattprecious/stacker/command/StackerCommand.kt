package com.mattprecious.stacker.command

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.jakewharton.mosaic.runMosaic
import com.mattprecious.stacker.rendering.LocalPrinter
import com.mattprecious.stacker.rendering.Printer
import com.mattprecious.stacker.vc.LibGit2Error
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.withContext

abstract class StackerCommand {
	internal sealed interface State {
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

	@Composable
	internal fun rememberWorkState(): WorkState {
		return remember { WorkState() }
	}

	@Stable
	internal class WorkState {
		var state by mutableStateOf<State>(State.Working)
	}

	@Composable
	internal fun Work(
		onFinish: (Boolean) -> Unit,
		workState: WorkState = rememberWorkState(),
	) {
		val printer = remember { Printer() }

		// Mosaic will wait for all effects to finish before exiting. Finished and Aborted signal that
		// we should terminate this collect in order to tear down.
		val currentState = remember {
			snapshotFlow { workState.state }
				.transformWhile {
					// We need to emit the terminal state so that the LaunchedEffect below can be interrupted.
					emit(it)
					it !is State.TerminalState
				}
		}
			.collectAsState(workState.state)
			.value

		printer.Messages()

		if (currentState is State.TerminalState) {
			SideEffect { onFinish(currentState is State.Finished) }
		} else {
			LaunchedEffect(Unit) {
				// Don't block the render thread.
				withContext(Dispatchers.IO) {
					with(StackerCommandScope(printer, workState)) {
						try {
							work()
						} catch (e: LibGit2Error) {
							printStatic(e.message!!)
							abort()
						}
					}

					workState.state = State.Finished
				}
			}
		}

		if (currentState is State.Rendering<*>) {
			CompositionLocalProvider(LocalPrinter provides printer) {
				currentState.content { workState.state = State.DeliveringRenderResult(it) }
			}
		}
	}

	protected open suspend fun StackerCommandScope.work() {}
}

suspend fun StackerCommand.run(): Boolean {
	var result = false

	runMosaic {
		Work(onFinish = { result = it })
	}

	return result
}
