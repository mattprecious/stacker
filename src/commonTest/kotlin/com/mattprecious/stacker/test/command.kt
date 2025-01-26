
import androidx.compose.runtime.snapshotFlow
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jakewharton.mosaic.Mosaic
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.testing.MosaicSnapshots
import com.jakewharton.mosaic.testing.TestMosaic
import com.jakewharton.mosaic.testing.runMosaicTest
import com.jakewharton.mosaic.ui.AnsiLevel
import com.mattprecious.stacker.command.StackerCommand
import com.mattprecious.stacker.command.StackerCommand.State
import com.mattprecious.stacker.command.StackerCommand.WorkState
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
	var result: Boolean? = null
		private set

	// So the IDE doesn't trim trailing spaces in test assertions...
	val s = " "

	private val state = WorkState()

	init {
		mosaic.setContentAndSnapshot {
			command.Work(state = state, onFinish = { result = it })
		}
	}

	fun sendText(text: String) {
		text.forEach { mosaic.sendKeyEvent(KeyEvent("$it")) }
	}

	fun sendKeyEvent(keyEvent: KeyEvent) {
		mosaic.sendKeyEvent(keyEvent)
	}

	suspend fun awaitFrame(
		output: String,
		static: String = "",
	) {
		withTimeout(1.seconds) {
			snapshotFlow { state.state.value }
				.filter { it is State.Rendering<*> || it is State.TerminalState }
				.first()
		}

		val snapshot = mosaic.awaitSnapshot()
		assertThat(snapshot.paintStatics().joinToString("\n") { it.render(AnsiLevel.NONE) })
			.isEqualTo(static)

		assertThat(snapshot.paint().render(AnsiLevel.NONE)).isEqualTo(output)

		// https://github.com/JakeWharton/mosaic/issues/663
		if (static.isNotEmpty() && result == null) {
			assertThat(mosaic.awaitSnapshot().paint().render(AnsiLevel.NONE)).isEqualTo(output)
		}
	}
}
