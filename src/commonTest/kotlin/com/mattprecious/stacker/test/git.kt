package com.mattprecious.stacker.test

import okio.Path

fun StackerTestScope.gitInit() {
	environment.exec("git init")
	environment.setGitDefaultBranch("main")
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
	environment.exec("git commit --allow-empty -m \"$message\"")
}

fun StackerTestScope.gitCreateAndSwitchToBranch(
	name: String,
) {
	environment.exec("git checkout -b $name")
}

fun StackerTestScope.gitHeadSha(): String {
	return environment.exec("git rev-parse HEAD")
}
