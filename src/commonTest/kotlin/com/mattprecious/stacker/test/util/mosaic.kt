package com.mattprecious.stacker.test.util

import androidx.compose.runtime.Composable
import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import com.jakewharton.mosaic.Mosaic
import com.jakewharton.mosaic.terminal.AnsiLevel
import com.jakewharton.mosaic.terminal.KeyboardEvent
import com.jakewharton.mosaic.testing.TestMosaic

val KeyboardEvent.Companion.Backspace get() = 127
val KeyboardEvent.Companion.Enter get() = 13

fun TestMosaic<Mosaic>.setContentWithStatics(
	content: @Composable () -> Unit,
): Mosaic {
	return setContentAndSnapshot {
		content()
	}
}

fun Assert<Mosaic>.matches(
	output: String? = null,
	static: String = "",
) {
	hasStaticsEqualTo(static)
	output?.let(::hasOutputEqualTo)
}

fun Assert<Mosaic>.hasOutputEqualTo(expected: String) {
	prop("output") { it.draw().render(AnsiLevel.NONE, supportsKittyUnderlines = false) }
		.isEqualTo(expected)
}

fun Assert<Mosaic>.hasStaticsEqualTo(expected: String) {
	prop("statics") { it.static() ?: "" }
		.isEqualTo(expected)
}

fun TestMosaic<*>.sendText(text: String) {
	text.forEach { sendKeyEvent(KeyboardEvent(it.code)) }
}
