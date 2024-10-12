package com.mattprecious.stacker.rendering

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import com.jakewharton.mosaic.ui.Static
import com.jakewharton.mosaic.ui.Text

val LocalPrinter = staticCompositionLocalOf { Printer() }

class Printer {
	private val messages = SnapshotStateList<String>()

	fun printStatic(message: String) {
		messages += message
	}

	@Composable
	fun Messages() {
		Static(messages) {
			Text(it)
		}
	}
}
