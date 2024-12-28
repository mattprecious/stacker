import com.jakewharton.mosaic.Mosaic
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
		scope.awaitOutput()

		scope.validate()
		scope.expectNoItems()
	}
}

class CommandTestScope internal constructor(
	command: StackerCommand,
	private val mosaic: TestMosaic<Mosaic>,
) {
	private val statics = ArrayDeque<String>()
	private val outputs = ArrayDeque<String>()
	private var result: Boolean? = null

	init {
		mosaic.setContent {
			command.Work(onFinish = { result = it })
		}
	}

	fun expectNoItems() {
		if (outputs.isNotEmpty()) {
			throw AssertionError("Expected no outputs but found \"${outputs.first()}\"")
		}

		if (statics.isNotEmpty()) {
			throw AssertionError("Expected no statics but found \"${statics.first()}\"")
		}

		if (result != null) {
			throw AssertionError("Expected no result but found $result")
		}
	}

	suspend fun awaitResult(): Boolean {
		while (result == null) {
			awaitAndProcessSnapshot()
		}

		return result!!.also { result = null }
	}

	suspend fun awaitOutput(): String {
		while (outputs.isEmpty()) {
			awaitAndProcessSnapshot()
		}

		return outputs.removeFirst()
	}

	suspend fun awaitStatic(): String {
		while (statics.isEmpty()) {
			awaitAndProcessSnapshot()
		}

		return statics.removeFirst()
	}

	private suspend fun awaitAndProcessSnapshot() {
		val snapshot = mosaic.awaitSnapshot()
		statics.addAll(snapshot.paintStatics().map { it.render(AnsiLevel.NONE) })
		outputs.add(snapshot.paint().render(AnsiLevel.NONE))
	}
}
