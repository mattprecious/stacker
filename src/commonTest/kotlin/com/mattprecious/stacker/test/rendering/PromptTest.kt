package com.mattprecious.stacker.test.rendering

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.testing.MosaicSnapshots
import com.jakewharton.mosaic.testing.runMosaicTest
import com.mattprecious.stacker.rendering.Prompt
import com.mattprecious.stacker.test.util.matches
import com.mattprecious.stacker.test.util.sendText
import com.mattprecious.stacker.test.util.setContentWithStatics
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class PromptTest {
	@Test
	fun emptyInputIsAllowed() = runTest {
		var result: String? = null

		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				Prompt(
					message = "Enter your name",
					hideInput = false,
					onSubmit = { result = it },
				)
			}

			assertThat(first).matches("Enter your name: ")

			sendKeyEvent(KeyEvent("Enter"))

			assertThat(awaitSnapshot()).matches(static = "Enter your name: ")
		}

		assertThat(result).isNotNull().isEmpty()
	}

	@Test
	fun nonEmptyInput() = runTest {
		var result: String? = null

		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				Prompt(
					message = "Enter your name",
					hideInput = false,
					onSubmit = { result = it },
				)
			}

			assertThat(first).matches("Enter your name: ")

			sendText("Mattt")
			assertThat(awaitSnapshot()).matches("Enter your name: Mattt")

			sendKeyEvent(KeyEvent("Backspace"))
			assertThat(awaitSnapshot()).matches("Enter your name: Matt")

			sendKeyEvent(KeyEvent("Enter"))
			assertThat(awaitSnapshot()).matches(static = "Enter your name: Matt")
		}

		assertThat(result).isNotNull().isEqualTo("Matt")
	}

	@Test
	fun hiddenInput() = runTest {
		var result: String? = null

		runMosaicTest(MosaicSnapshots) {
			var forceRecompose by mutableIntStateOf(0)
			val first = setContentWithStatics {
				LaunchedEffect(forceRecompose) {}
				Prompt(
					message = "Enter your name",
					hideInput = true,
					onSubmit = { result = it },
				)
			}

			assertThat(first).matches("Enter your name: ")

			sendText("Matt")
			forceRecompose++
			assertThat(awaitSnapshot()).matches("Enter your name: ")

			sendKeyEvent(KeyEvent("Enter"))
			assertThat(awaitSnapshot()).matches(static = "Enter your name: ")
		}

		assertThat(result).isNotNull().isEqualTo("Matt")
	}
}
