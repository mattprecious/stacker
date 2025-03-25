package com.mattprecious.stacker.test.rendering

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jakewharton.mosaic.terminal.KeyboardEvent
import com.jakewharton.mosaic.testing.MosaicSnapshots
import com.jakewharton.mosaic.testing.runMosaicTest
import com.mattprecious.stacker.rendering.InteractivePrompt
import com.mattprecious.stacker.rendering.PromptState
import com.mattprecious.stacker.rendering.toAnnotatedString
import com.mattprecious.stacker.test.util.Backspace
import com.mattprecious.stacker.test.util.Enter
import com.mattprecious.stacker.test.util.hasStaticsEqualTo
import com.mattprecious.stacker.test.util.matches
import com.mattprecious.stacker.test.util.sendText
import com.mattprecious.stacker.test.util.setContentWithStatics
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class InteractivePromptTest {
	@Test
	fun firstOptionIsHighlighted() = runTest {
		var result: String? = null

		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				InteractivePrompt(
					message = "Select an animal",
					state = rememberTestStateAnimals(),
					filteringEnabled = false,
					onSelected = { result = it },
				)
			}

			assertThat(first).matches(
				"""
				|Select an animal:
				|❯ Lion
				|  Tiger
				|  Bear
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			assertThat(awaitSnapshot()).hasStaticsEqualTo("Select an animal: Lion")
		}

		assertThat(result).isEqualTo("Lion")
	}

	@Test
	fun defaultOptionIsHighlighted() = runTest {
		var result: String? = null

		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				InteractivePrompt(
					message = "Select an animal",
					state = rememberTestStateAnimals(default = "Tiger"),
					filteringEnabled = false,
					onSelected = { result = it },
				)
			}

			assertThat(first).matches(
				"""
				|Select an animal:
				|  Lion
				|❯ Tiger
				|  Bear
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			assertThat(awaitSnapshot()).hasStaticsEqualTo("Select an animal: Tiger")
		}

		assertThat(result).isEqualTo("Tiger")
	}

	@Test
	fun invalidDefaultFallsBackToFirstOption() = runTest {
		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				InteractivePrompt(
					message = "Select an animal",
					state = rememberTestStateAnimals(default = "Zebra"),
					filteringEnabled = false,
					onSelected = {},
				)
			}

			assertThat(first).matches(
				"""
				|Select an animal:
				|❯ Lion
				|  Tiger
				|  Bear
				""".trimMargin(),
			)
		}
	}

	@Test
	fun nullMessage() = runTest {
		var result: String? = null

		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				InteractivePrompt(
					state = rememberTestStateAnimals(),
					filteringEnabled = false,
					onSelected = { result = it },
				)
			}

			assertThat(first).matches(
				"""
				|❯ Lion
				|  Tiger
				|  Bear
				""".trimMargin(),
			)
		}
	}

	@Test
	fun arrowSelection() = runTest {
		var result: String? = null

		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				InteractivePrompt(
					message = "Select an animal",
					state = rememberTestStateAnimals(),
					filteringEnabled = false,
					onSelected = { result = it },
				)
			}

			assertThat(first).matches(
				"""
				|Select an animal:
				|❯ Lion
				|  Tiger
				|  Bear
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:
				|  Lion
				|❯ Tiger
				|  Bear
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:
				|  Lion
				|  Tiger
				|❯ Bear
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Up))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:
				|  Lion
				|❯ Tiger
				|  Bear
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			assertThat(awaitSnapshot()).hasStaticsEqualTo("Select an animal: Tiger")
		}

		assertThat(result).isEqualTo("Tiger")
	}

	@Test
	fun transformations() = runTest {
		var result: String? = null

		class Animal(
			val adjective: String,
			val name: String,
		)

		val animals = persistentListOf(
			Animal("Lazy", "Lion"),
			Animal("Timid", "Tiger"),
			Animal("Brave", "Bear"),
		)

		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				InteractivePrompt(
					message = "Select an animal",
					state = remember {
						PromptState(
							options = animals,
							default = null,
							displayTransform = { "${it.adjective} ${it.name}".toAnnotatedString() },
							valueTransform = { it.name.toAnnotatedString() },
						)
					},
					filteringEnabled = false,
					onSelected = { result = it.name },
				)
			}

			assertThat(first).matches(
				"""
				|Select an animal:
				|❯ Lazy Lion
				|  Timid Tiger
				|  Brave Bear
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			assertThat(awaitSnapshot()).hasStaticsEqualTo("Select an animal: Lion")
		}

		assertThat(result).isEqualTo("Lion")
	}

	@Test
	fun filteringDisabled() = runTest {
		runMosaicTest(MosaicSnapshots) {
			var forceRecompose by mutableIntStateOf(0)
			val first = setContentWithStatics {
				LaunchedEffect(forceRecompose) {}
				InteractivePrompt(
					message = "Select an animal",
					state = rememberTestStateAnimals(),
					filteringEnabled = false,
					onSelected = {},
				)
			}

			assertThat(first).matches(
				"""
				|Select an animal:
				|❯ Lion
				|  Tiger
				|  Bear
				""".trimMargin(),
			)

			sendText("bear")
			forceRecompose++

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:
				|❯ Lion
				|  Tiger
				|  Bear
				""".trimMargin(),
			)
		}
	}

	@Test
	fun basicFilterAndSelect() = runTest {
		var result: String? = null

		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				InteractivePrompt(
					message = "Select an animal",
					state = rememberTestStateAnimals(),
					filteringEnabled = true,
					onSelected = { result = it },
				)
			}

			assertThat(first).matches(
				"""
				|Select an animal:
				|❯ Lion
				|  Tiger
				|  Bear
				""".trimMargin(),
			)

			sendText("i")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: i
				|❯ Lion
				|  Tiger
				""".trimMargin(),
			)

			sendText("o")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: io
				|❯ Lion
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			assertThat(awaitSnapshot()).hasStaticsEqualTo("Select an animal: Lion")
		}

		assertThat(result).isEqualTo("Lion")
	}

	@Test
	fun filteringWithNullMessage() = runTest {
		var result: String? = null

		runMosaicTest(MosaicSnapshots) {
			var forceRecompose by mutableIntStateOf(0)
			val first = setContentWithStatics {
				LaunchedEffect(forceRecompose) {}
				InteractivePrompt(
					state = rememberTestStateAnimals(),
					filteringEnabled = true,
					onSelected = { result = it },
				)
			}

			assertThat(first).matches(
				"""
				|
				|❯ Lion
				|  Tiger
				|  Bear
				""".trimMargin(),
			)

			sendText("io")

			assertThat(awaitSnapshot()).matches(
				"""
				|io
				|❯ Lion
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			forceRecompose++
			assertThat(awaitSnapshot()).hasStaticsEqualTo("")
		}

		assertThat(result).isEqualTo("Lion")
	}

	@Test
	fun resultNotPrinted() = runTest {
		var result: String? = null

		runMosaicTest(MosaicSnapshots) {
			var forceRecompose by mutableIntStateOf(0)
			val first = setContentWithStatics {
				LaunchedEffect(forceRecompose) {}
				InteractivePrompt(
					message = "Select an animal",
					state = rememberTestStateAnimals(),
					filteringEnabled = true,
					staticPrintResult = false,
					onSelected = { result = it },
				)
			}

			assertThat(first).matches(
				"""
				|Select an animal:
				|❯ Lion
				|  Tiger
				|  Bear
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			forceRecompose++
			assertThat(awaitSnapshot()).hasStaticsEqualTo("")
		}

		assertThat(result).isEqualTo("Lion")
	}

	@Test
	fun filterToEmpty() = runTest {
		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				InteractivePrompt(
					message = "Select an animal",
					state = rememberTestStateAnimals(),
					filteringEnabled = true,
					onSelected = {},
				)
			}

			assertThat(first).matches(
				"""
				|Select an animal:
				|❯ Lion
				|  Tiger
				|  Bear
				""".trimMargin(),
			)

			sendText("zebra")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: zebra
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Backspace))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Backspace))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Backspace))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Backspace))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Backspace))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:
				|❯ Lion
				|  Tiger
				|  Bear
				""".trimMargin(),
			)
		}
	}

	@Test
	fun selectionMovesToNextOptionWhenFilterRemovesCurrent() = runTest {
		var result: String? = null

		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				InteractivePrompt(
					message = "Select an animal",
					state = rememberTestStateAnimals(),
					filteringEnabled = true,
					onSelected = { result = it },
				)
			}

			assertThat(first).matches(
				"""
				|Select an animal:
				|❯ Lion
				|  Tiger
				|  Bear
				""".trimMargin(),
			)

			sendText("e")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: e
				|❯ Tiger
				|  Bear
				""".trimMargin(),
			)

			sendText("a")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: ea
				|❯ Bear
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Enter))

			assertThat(awaitSnapshot()).hasStaticsEqualTo("Select an animal: Bear")
		}

		assertThat(result).isEqualTo("Bear")
	}

	@Test
	fun selectionIsRestoredWhenUnfiltering() = runTest {
		runMosaicTest(MosaicSnapshots) {
			setContentWithStatics {
				InteractivePrompt(
					message = "Select an animal",
					state = rememberTestStateAnimals(),
					filteringEnabled = true,
					onSelected = {},
				)
			}

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:
				|  Lion
				|  Tiger
				|❯ Bear
				""".trimMargin(),
			)

			sendText("io")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: io
				|❯ Lion
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Backspace))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: i
				|  Lion
				|❯ Tiger
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Backspace))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:
				|  Lion
				|  Tiger
				|❯ Bear
				""".trimMargin(),
			)
		}
	}

	@Test
	fun selectionStackIsResetWhenArrowing() = runTest {
		runMosaicTest(MosaicSnapshots) {
			setContentWithStatics {
				InteractivePrompt(
					message = "Select an animal",
					state = rememberTestStateAnimals(),
					filteringEnabled = true,
					onSelected = {},
				)
			}

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:
				|  Lion
				|  Tiger
				|❯ Bear
				""".trimMargin(),
			)

			sendText("io")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: io
				|❯ Lion
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Backspace))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: i
				|❯ Lion
				|  Tiger
				""".trimMargin(),
			)

			sendKeyEvent(KeyboardEvent(KeyboardEvent.Backspace))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:
				|❯ Lion
				|  Tiger
				|  Bear
				""".trimMargin(),
			)
		}
	}

	@Test
	fun selectionMovesToClosesRemainingSibling() = runTest {
		runMosaicTest(MosaicSnapshots) {
			val first = setContentWithStatics {
				InteractivePrompt(
					message = "Select an animal",
					state = remember {
						PromptState(
							options = persistentListOf(
								"Aardvark",
								"Bear",
								"Chicken",
								"Dog",
								"Elephant",
								"Fox",
								"Goat",
								"Horse",
								"Iguana",
								"Jaguar",
							),
							default = "Fox",
							displayTransform = { it.toAnnotatedString() },
							valueTransform = { it.toAnnotatedString() },
						)
					},
					filteringEnabled = true,
					onSelected = {},
				)
			}

			assertThat(first).matches(
				"""
				|Select an animal:
				|  Aardvark
				|  Bear
				|  Chicken
				|  Dog
				|  Elephant
				|❯ Fox
				|  Goat
				|  Horse
				|  Iguana
				|  Jaguar
				""".trimMargin(),
			)

			sendText("a")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: a
				|  Aardvark
				|  Bear
				|❯ Elephant
				|  Goat
				|  Iguana
				|  Jaguar
				""".trimMargin(),
			)

			sendText("r")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: ar
				|  Aardvark
				|❯ Bear
				|  Jaguar
				""".trimMargin(),
			)

			// Reset and change starting selection.
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Backspace))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Backspace))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))
			sendKeyEvent(KeyboardEvent(KeyboardEvent.Down))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:
				|  Aardvark
				|  Bear
				|  Chicken
				|  Dog
				|  Elephant
				|  Fox
				|  Goat
				|  Horse
				|❯ Iguana
				|  Jaguar
				""".trimMargin(),
			)

			sendText("a")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: a
				|  Aardvark
				|  Bear
				|  Elephant
				|  Goat
				|❯ Iguana
				|  Jaguar
				""".trimMargin(),
			)

			sendText("r")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: ar
				|  Aardvark
				|  Bear
				|❯ Jaguar
				""".trimMargin(),
			)
		}
	}

	@Composable
	private fun rememberTestStateAnimals(
		default: String? = null,
	): PromptState<String> {
		return remember {
			PromptState(
				options = persistentListOf(
					"Lion",
					"Tiger",
					"Bear",
				),
				default = default,
				displayTransform = { it.toAnnotatedString() },
				valueTransform = { it.toAnnotatedString() },
			)
		}
	}
}
