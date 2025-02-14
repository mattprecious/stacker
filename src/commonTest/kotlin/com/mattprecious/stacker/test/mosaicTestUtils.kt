package com.mattprecious.stacker.test

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show
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

fun Assert<Mosaic>.hasOutputEqualTo(expected: String) = given { actual ->
	val renderedOutput = actual.paint().render(AnsiLevel.NONE)
	if (renderedOutput != expected) {
		expected("output:${show(expected)} but was output:${show(renderedOutput)}")
	}
}

fun Assert<Mosaic>.hasStaticsEqualTo(expected: String) = given { actual ->
	val renderedStatics = actual.paintStatics().joinToString("\n") { it.render(AnsiLevel.NONE) }
	if (renderedStatics != expected) {
		expected("static:${show(expected)} but was static:${show(renderedStatics)}")
	}
}

fun TestMosaic<*>.sendText(text: String) {
	text.forEach { sendKeyEvent(KeyEvent("$it")) }
}
