package com.mattprecious.stacker.rendering

import com.jakewharton.mosaic.text.AnnotatedString
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.TextStyle
import com.jakewharton.mosaic.ui.TextStyle.Companion.Italic

private val branchStyle = SpanStyle(
	color = Color(189, 147, 249),
	textStyle = Italic,
)

private val codeStyle = SpanStyle(
	color = Color(97, 175, 239),
)

fun AnnotatedString.Builder.branch(content: AnnotatedString.Builder.() -> Unit) {
	withStyle(branchStyle, content)
}

fun AnnotatedString.Builder.code(content: AnnotatedString.Builder.() -> Unit) {
	withStyle(codeStyle, content)
}

fun AnnotatedString.Builder.promptItem(
	selected: Boolean,
	content: AnnotatedString.Builder.() -> Unit,
) {
	if (selected) {
		append("❯ ")
		withStyle(SpanStyle(textStyle = TextStyle.Underline), content)
	} else {
		append("  ")
		content()
	}
}
fun String.toAnnotatedString() = buildAnnotatedString { append(this@toAnnotatedString) }
