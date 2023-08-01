package com.mattprecious.stacker.vc

import java.nio.file.Path

interface VersionControl {
	val root: Path
	val configDirectory: Path

	fun fallthrough(commands: List<String>)
}
