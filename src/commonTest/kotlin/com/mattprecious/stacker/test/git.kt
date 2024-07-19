package com.mattprecious.stacker.test

import okio.Path

fun StackerTestScope.gitInit() {
	environment.exec("git init")
}

fun StackerTestScope.gitAdd(
	vararg files: Path,
) {
	environment.exec("git add ${files.joinToString(" ")}")
}

fun StackerTestScope.gitCommit(
	message: String,
) {
	// TODO: Escaping.
	environment.exec("git commit -m \"$message\"")
}

fun StackerTestScope.gitHeadSha(): String {
  return environment.exec("git rev-parse HEAD")
}
