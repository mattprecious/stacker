package com.mattprecious.stacker.rendering

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.colormath.model.RGB
import com.github.ajalt.mordant.rendering.TextStyle

private val branch = TextStyle(
	color = RGB("#bd93f9"),
	italic = true,
)

private val code = TextStyle(
	color = RGB("#61afef"),
)

context(CliktCommand)
fun String.styleBranch(): String {
	return branch(this)
}

context(CliktCommand)
fun String.styleCode(): String {
	return code(this)
}
