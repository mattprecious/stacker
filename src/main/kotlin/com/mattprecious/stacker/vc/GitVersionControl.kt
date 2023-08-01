package com.mattprecious.stacker.vc

import com.mattprecious.stacker.shell.Shell
import java.nio.file.Path
import kotlin.io.path.div

class GitVersionControl(
	private val shell: Shell,
) : VersionControl {
	override val root: Path by lazy {
		Path.of(shell.exec(COMMAND, "rev-parse", "--show-toplevel"))
	}

	override val configDirectory: Path
		get() = root / ".git/stacker"

	override fun fallthrough(commands: List<String>) {
		shell.exec(COMMAND, *commands.toTypedArray())
	}

	companion object {
		private const val COMMAND = "git"
	}
}
