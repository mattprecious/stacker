package com.mattprecious.stacker.test.rendering

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import app.cash.burst.Burst
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.jakewharton.mosaic.terminal.KeyboardEvent
import com.jakewharton.mosaic.testing.MosaicSnapshots
import com.jakewharton.mosaic.testing.runMosaicTest
import com.mattprecious.stacker.rendering.YesNoPrompt
import com.mattprecious.stacker.test.util.Backspace
import com.mattprecious.stacker.test.util.Enter
import com.mattprecious.stacker.test.util.matches
import com.mattprecious.stacker.test.util.sendText
import com.mattprecious.stacker.test.util.setContentWithStatics
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@Burst
class YesNoPromptTest {
	@Test
	fun invalidInputsAreIgnored() = runTest {
		var result: Boolean? = null

		runMosaicTest(MosaicSnapshots) {
			var forceRecompose by mutableIntStateOf(0)
			val first = setContentWithStatics {
				LaunchedEffect(forceRecompose) {}
				YesNoPrompt(
					message = "Yes or no?",
					default = null,
					onSubmit = { result = it },
				)
			}

			assertThat(first).matches("Yes or no? [y/n]: ")

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))
			forceRecompose++

			assertThat(awaitSnapshot()).matches("Yes or no? [y/n]: ")

			sendText("a")
			assertThat(awaitSnapshot()).matches("Yes or no? [y/n]: a")

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))
			assertThat(awaitSnapshot()).matches("Yes or no? [y/n]: ")

			sendText("1")
			assertThat(awaitSnapshot()).matches("Yes or no? [y/n]: 1")

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))
			assertThat(awaitSnapshot()).matches("Yes or no? [y/n]: ")

			sendText("yes")
			assertThat(awaitSnapshot()).matches("Yes or no? [y/n]: yes")

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))
			assertThat(awaitSnapshot()).matches("Yes or no? [y/n]: ")
		}

		assertThat(result).isNull()
	}

	enum class ValidInputCase(
		val input: String,
		val result: Boolean,
	) {
		LowerY("y", true),
		UpperY("Y", true),
		LowerN("n", false),
		UpperN("N", false),
	}

	@Test
	fun validInputs(
		case: ValidInputCase,
	) = runTest {
		var result: Boolean? = null

		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				YesNoPrompt(
					message = "Yes or no?",
					default = null,
					onSubmit = { result = it },
				)
			}

			assertThat(first).matches("Yes or no? [y/n]: ")

			sendText(case.input)

			assertThat(awaitSnapshot()).matches("Yes or no? [y/n]: ${case.input}")

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			assertThat(awaitSnapshot()).matches(static = "Yes or no? [y/n]: ${case.input}")
		}

		assertThat(result).isEqualTo(case.result)
	}

	@Test
	fun defaultTrue() = runTest {
		var result: Boolean? = null

		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				YesNoPrompt(
					message = "Yes or no?",
					default = true,
					onSubmit = { result = it },
				)
			}

			assertThat(first).matches("Yes or no? [Y/n]: ")

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			assertThat(awaitSnapshot()).matches(static = "Yes or no? [Y/n]: ")
		}

		assertThat(result).isNotNull().isTrue()
	}

	@Test
	fun defaultFalse() = runTest {
		var result: Boolean? = null

		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				YesNoPrompt(
					message = "Yes or no?",
					default = false,
					onSubmit = { result = it },
				)
			}

			assertThat(first).matches("Yes or no? [y/N]: ")

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			assertThat(awaitSnapshot()).matches(static = "Yes or no? [y/N]: ")
		}

		assertThat(result).isNotNull().isFalse()
	}

	@Test
	fun backspace() = runTest {
		var result: Boolean? = null

		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				YesNoPrompt(
					message = "Yes or no?",
					default = null,
					onSubmit = { result = it },
				)
			}

			assertThat(first).matches("Yes or no? [y/n]: ")

			sendText("y")
			assertThat(awaitSnapshot()).matches("Yes or no? [y/n]: y")

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Backspace))
			assertThat(awaitSnapshot()).matches("Yes or no? [y/n]: ")
		}

		assertThat(result).isNull()
	}
}
