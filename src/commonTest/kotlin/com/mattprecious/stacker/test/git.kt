package com.mattprecious.stacker.test

import okio.Path

fun TestEnvironment.gitInit() {
	environment.exec("git init")
}

fun TestEnvironment.gitAdd(
	vararg files: Path,
) {
	environment.exec("git add ${files.joinToString(" ")}")
}

fun TestEnvironment.gitCommit(
	message: String,
) {
	// TODO: Escaping.
	environment.exec("git commit -m \"$message\"")
}

fun TestEnvironment.gitHeadSha(): String {
	return environment.exec("git rev-parse HEAD")
}
