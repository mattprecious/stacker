import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jakewharton.mosaic.Mosaic
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.testing.MosaicSnapshots
import com.jakewharton.mosaic.testing.TestMosaic
import com.jakewharton.mosaic.testing.runMosaicTest
import com.jakewharton.mosaic.ui.AnsiLevel
import com.mattprecious.stacker.command.StackerCommand

suspend fun StackerCommand.test(
	validate: suspend CommandTestScope.() -> Unit,
) {
	runMosaicTest(MosaicSnapshots) {
		val scope = CommandTestScope(command = this@test, mosaic = this)

		// The first snapshot will always be empty since command execution is started inside a
		// launched effect. Discard it.
		scope.awaitFrame("")

		scope.validate()
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

	init {
		mosaic.setContent {
			command.Work(onFinish = { result = it })
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
		static: String = ""
	) {
		val snapshot = mosaic.awaitSnapshot()
		assertThat(snapshot.paintStatics().joinToString("\n") { it.render(AnsiLevel.NONE) })
			.isEqualTo(static)

		assertThat(snapshot.paint().render(AnsiLevel.NONE)).isEqualTo(output)
	}
}
