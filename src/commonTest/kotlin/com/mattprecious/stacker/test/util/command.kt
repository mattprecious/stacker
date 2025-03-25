
import androidx.compose.runtime.snapshotFlow
import assertk.assertThat
import com.jakewharton.mosaic.Mosaic
import com.jakewharton.mosaic.terminal.KeyboardEvent
import com.jakewharton.mosaic.terminal.Terminal
import com.jakewharton.mosaic.testing.MosaicSnapshots
import com.jakewharton.mosaic.testing.TestMosaic
import com.jakewharton.mosaic.testing.runMosaicTest
import com.jakewharton.mosaic.ui.unit.IntSize
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommand.State
import com.mattprecious.stacker.command.StackerCommand.WorkState
import com.mattprecious.stacker.test.util.matches
import com.mattprecious.stacker.test.util.sendText
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.seconds

suspend fun StackerCommand.test(
	validate: suspend CommandTestScope.() -> Unit,
) {
	runMosaicTest(MosaicSnapshots) {
		CommandTestScope(command = this@test, mosaic = this).validate()
	}
}

class CommandTestScope internal constructor(
	command: StackerCommand,
	private val mosaic: TestMosaic<Mosaic>,
) {
	private var result: Boolean? = null

	private val state = WorkState()

	init {
		val snapshot = mosaic.setContentAndSnapshot {
			command.Work(workState = state, onFinish = { result = it })
		}

		// Because of our state machine internals, the first snapshot will always be blank.
		assertThat(snapshot).matches("")
	}

	fun sendText(text: String) {
		mosaic.sendText(text)
	}

	fun sendKeyEvent(keyEvent: KeyboardEvent) {
		mosaic.sendKeyEvent(keyEvent)
	}

	fun setSize(size: IntSize) {
		mosaic.state.size.value = Terminal.Size(columns = size.width, rows = size.height)
	}

	suspend fun awaitFrame(
		output: String,
		static: String = "",
	) {
		withTimeout(1.seconds) {
			snapshotFlow { state.state }
				.filter { it is State.Rendering<*> || it is State.TerminalState }
				.first()
		}

		assertThat(mosaic.awaitSnapshot()).matches(output, static)
	}

	suspend fun awaitResult(): Boolean {
		result?.let { return it }

		withTimeout(1.seconds) {
			snapshotFlow { state.state }
				.filter { it is State.TerminalState }
				.first()
		}

		// The internal state machine of StackerCommand requires an extra composition in order to
		// terminate. This composition will emit an empty frame.
		awaitFrame("")

		return result!!
	}
}
