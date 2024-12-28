package com.mattprecious.stacker.rendering

import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.mordant.rendering.TextStyle
import com.jakewharton.mosaic.text.AnnotatedString
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.TextStyle.Companion.Italic

private val branch = TextStyle(
	color = RGB("#bd93f9"),
	italic = true,
)

private val branchStyle = SpanStyle(
	color = Color(189, 147, 249),
	textStyle = Italic,
)

private val code = TextStyle(
	color = RGB("#61afef"),
)

private val codeStyle = SpanStyle(
	color = Color(97, 175, 239),
)

fun String.styleBranch(): String {
	return branch(this)
}

fun String.styleCode(): String {
	return code(this)
}

fun AnnotatedString.Builder.branch(content: AnnotatedString.Builder.() -> Unit) {
	withStyle(branchStyle, content)
}

fun AnnotatedString.Builder.code(content: AnnotatedString.Builder.() -> Unit) {
	withStyle(codeStyle, content)
}

private fun AnnotatedString.Builder.withStyle(
	style: SpanStyle,
	content: AnnotatedString.Builder.() -> Unit,
) {
	pushStyle(style)
	content()
	pop()
}
