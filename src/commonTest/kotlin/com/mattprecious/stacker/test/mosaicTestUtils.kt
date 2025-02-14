package com.mattprecious.stacker.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import com.jakewharton.mosaic.Mosaic
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.testing.TestMosaic
import com.jakewharton.mosaic.ui.AnsiLevel
import com.mattprecious.stacker.rendering.LocalPrinter
import com.mattprecious.stacker.rendering.Printer

// So the IDE doesn't trim trailing spaces in test assertions...
val s = " "

fun TestMosaic<Mosaic>.setContentWithStatics(
	content: @Composable () -> Unit,
): Mosaic {
	return setContentAndSnapshot {
		CompositionLocalProvider(
			LocalPrinter provides Printer(),
		) {
			LocalPrinter.current.Messages()
			content()
		}
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
	prop("output") { it.paint().render(AnsiLevel.NONE) }.isEqualTo(expected)
}

fun Assert<Mosaic>.hasStaticsEqualTo(expected: String) {
	prop("statics") { it.paintStatics().joinToString("\n") { it.render(AnsiLevel.NONE) } }
		.isEqualTo(expected)
}

fun TestMosaic<*>.sendText(text: String) {
	text.forEach { sendKeyEvent(KeyEvent("$it")) }
}
