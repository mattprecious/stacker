// Copyright 2018 AJ Alt
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

@file:OptIn(ExperimentalForeignApi::class)

package com.mattprecious.stacker.command.downstack

import com.mattprecious.stacker.command.StackerCommandScope
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.errno
import platform.posix.fclose
import platform.posix.fgets
import platform.posix.fopen
import platform.posix.fputs
import platform.posix.getenv
import platform.posix.remove
import platform.posix.stat
import platform.posix.system
import platform.posix.tmpnam

internal class Editor(
	private val commandScope: StackerCommandScope,
	private val editorPath: String?,
	private val requireSave: Boolean,
	private val extension: String,
) {
	private fun getEditorPath(): String {
		val nul = "/dev/null"
		return editorPath ?: inferEditorPath { editor ->
			system("which $editor >$nul 2>$nul") == 0
		}
	}

	private suspend fun editFileWithEditor(editorCmd: String, filename: String) {
		val exitCode = system("$editorCmd $filename")
		if (exitCode != 0) {
			commandScope.printStaticError(
				"${editorCmd.takeWhile { !it.isWhitespace() }}: Editing failed!",
			)
			commandScope.abort()
		}
	}

	suspend fun edit(text: String): String? = memScoped {
		var filename = "${
			tmpnam(null)!!.toKString().trimEnd('.').replace("\\", "/")
		}.${extension.trimStart('.')}"

		val file = fopen(filename, "w")
		if (file == null) {
			commandScope.printStaticError("Error creating temporary file (errno=$errno)")
			commandScope.abort()
		}

		try {
			val editorCmd = getEditorPath()
			fputs(normalizeEditorText(editorCmd, text), file)
			fclose(file)

			val lastModified = getModificationTime(filename)

			editFileWithEditor(editorCmd, filename)

			if (requireSave && getModificationTime(filename) == lastModified) {
				return null
			}

			val replaceResult = readFileIfExists(filename)?.replace("\r\n", "\n")
			if (replaceResult == null) {
				commandScope.printStaticError("Could not read file")
				commandScope.abort()
			}

			return replaceResult
		} finally {
			remove(filename)
		}
	}
}

internal fun MemScope.getModificationTime(filename: String): Long {
	val stat = alloc<stat>()
	stat(filename, stat.ptr)
	return stat.st_mtimespec.tv_nsec
}

internal fun normalizeEditorText(editor: String, text: String): String {
	return when (editor) {
		"notepad" -> text.replace(Regex("(?<!\r)\n"), "\r\n")
		else -> text.replace("\r\n", "\n")
	}
}

internal fun inferEditorPath(commandExists: (String) -> Boolean): String {
	for (key in arrayOf("VISUAL", "EDITOR")) {
		return getenv(key)?.toKString() ?: continue
	}

	val editors = arrayOf("vim", "nano")

	for (editor in editors) {
		if (commandExists(editor)) return editor
	}

	return "vi"
}

internal fun readFileIfExists(filename: String): String? {
	val file = fopen(filename, "r") ?: return null
	val chunks = StringBuilder()
	try {
		memScoped {
			val bufferLength = 64 * 1024
			val buffer = allocArray<ByteVar>(bufferLength)

			while (true) {
				val chunk = fgets(buffer, bufferLength, file)?.toKString()
				if (chunk.isNullOrEmpty()) break
				chunks.append(chunk)
			}
		}
	} finally {
		fclose(file)
	}
	return chunks.toString()
}
