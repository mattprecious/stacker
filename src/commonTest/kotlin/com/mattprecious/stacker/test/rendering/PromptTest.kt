package com.mattprecious.stacker.test.rendering

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.testing.MosaicSnapshots
import com.jakewharton.mosaic.testing.runMosaicTest
import com.mattprecious.stacker.rendering.InteractivePrompt
import com.mattprecious.stacker.rendering.PromptState
import com.mattprecious.stacker.test.util.hasStaticsEqualTo
import com.mattprecious.stacker.test.util.matches
import com.mattprecious.stacker.test.util.s
import com.mattprecious.stacker.test.util.sendText
import com.mattprecious.stacker.test.util.setContentWithStatics
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class PromptTest {
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
				|Select an animal:$s
				|❯ Lion           $s
				|  Tiger          $s
				|  Bear           $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

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
				|Select an animal:$s
				|  Lion           $s
				|❯ Tiger          $s
				|  Bear           $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

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
				|Select an animal:$s
				|❯ Lion           $s
				|  Tiger          $s
				|  Bear           $s
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
				|Select an animal:$s
				|❯ Lion           $s
				|  Tiger          $s
				|  Bear           $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:$s
				|  Lion           $s
				|❯ Tiger          $s
				|  Bear           $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:$s
				|  Lion           $s
				|  Tiger          $s
				|❯ Bear           $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowUp"))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:$s
				|  Lion           $s
				|❯ Tiger          $s
				|  Bear           $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

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
							displayTransform = { "${it.adjective} ${it.name}" },
							valueTransform = { it.name },
						)
					},
					filteringEnabled = false,
					onSelected = { result = it.name },
				)
			}

			assertThat(first).matches(
				"""
				|Select an animal:$s
				|❯ Lazy Lion      $s
				|  Timid Tiger    $s
				|  Brave Bear     $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

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
				|Select an animal:$s
				|❯ Lion           $s
				|  Tiger          $s
				|  Bear           $s
				""".trimMargin(),
			)

			sendText("bear")
			forceRecompose++

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:$s
				|❯ Lion           $s
				|  Tiger          $s
				|  Bear           $s
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
				|Select an animal:$s
				|❯ Lion           $s
				|  Tiger          $s
				|  Bear           $s
				""".trimMargin(),
			)

			sendText("i")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: i
				|❯ Lion            $s
				|  Tiger           $s
				""".trimMargin(),
			)

			sendText("o")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: io
				|❯ Lion             $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

			assertThat(awaitSnapshot()).hasStaticsEqualTo("Select an animal: Lion")
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
				|Select an animal:$s
				|❯ Lion           $s
				|  Tiger          $s
				|  Bear           $s
				""".trimMargin(),
			)

			sendText("zebra")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: zebra
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))
			sendKeyEvent(KeyEvent("Backspace"))
			sendKeyEvent(KeyEvent("Backspace"))
			sendKeyEvent(KeyEvent("Backspace"))
			sendKeyEvent(KeyEvent("Backspace"))
			sendKeyEvent(KeyEvent("Backspace"))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:$s
				|❯ Lion           $s
				|  Tiger          $s
				|  Bear           $s
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
				|Select an animal:$s
				|❯ Lion           $s
				|  Tiger          $s
				|  Bear           $s
				""".trimMargin(),
			)

			sendText("e")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: e
				|❯ Tiger           $s
				|  Bear            $s
				""".trimMargin(),
			)

			sendText("a")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: ea
				|❯ Bear             $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Enter"))

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

			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("ArrowDown"))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:$s
				|  Lion           $s
				|  Tiger          $s
				|❯ Bear           $s
				""".trimMargin(),
			)

			sendText("io")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: io
				|❯ Lion             $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Backspace"))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: i
				|  Lion            $s
				|❯ Tiger           $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Backspace"))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:$s
				|  Lion           $s
				|  Tiger          $s
				|❯ Bear           $s
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

			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("ArrowDown"))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:$s
				|  Lion           $s
				|  Tiger          $s
				|❯ Bear           $s
				""".trimMargin(),
			)

			sendText("io")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: io
				|❯ Lion             $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("Backspace"))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: i
				|❯ Lion            $s
				|  Tiger           $s
				""".trimMargin(),
			)

			sendKeyEvent(KeyEvent("Backspace"))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:$s
				|❯ Lion           $s
				|  Tiger          $s
				|  Bear           $s
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
							displayTransform = { it },
							valueTransform = { it },
						)
					},
					filteringEnabled = true,
					onSelected = {},
				)
			}

			assertThat(first).matches(
				"""
				|Select an animal:$s
				|  Aardvark       $s
				|  Bear           $s
				|  Chicken        $s
				|  Dog            $s
				|  Elephant       $s
				|❯ Fox            $s
				|  Goat           $s
				|  Horse          $s
				|  Iguana         $s
				|  Jaguar         $s
				""".trimMargin(),
			)

			sendText("a")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: a
				|  Aardvark        $s
				|  Bear            $s
				|❯ Elephant        $s
				|  Goat            $s
				|  Iguana          $s
				|  Jaguar          $s
				""".trimMargin(),
			)

			sendText("r")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: ar
				|  Aardvark         $s
				|❯ Bear             $s
				|  Jaguar           $s
				""".trimMargin(),
			)

			// Reset and change starting selection.
			sendKeyEvent(KeyEvent("Backspace"))
			sendKeyEvent(KeyEvent("Backspace"))
			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("ArrowDown"))
			sendKeyEvent(KeyEvent("ArrowDown"))

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal:$s
				|  Aardvark       $s
				|  Bear           $s
				|  Chicken        $s
				|  Dog            $s
				|  Elephant       $s
				|  Fox            $s
				|  Goat           $s
				|  Horse          $s
				|❯ Iguana         $s
				|  Jaguar         $s
				""".trimMargin(),
			)

			sendText("a")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: a
				|  Aardvark        $s
				|  Bear            $s
				|  Elephant        $s
				|  Goat            $s
				|❯ Iguana          $s
				|  Jaguar          $s
				""".trimMargin(),
			)

			sendText("r")

			assertThat(awaitSnapshot()).matches(
				"""
				|Select an animal: ar
				|  Aardvark         $s
				|  Bear             $s
				|❯ Jaguar           $s
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
				displayTransform = { it },
				valueTransform = { it },
			)
		}
	}
}
