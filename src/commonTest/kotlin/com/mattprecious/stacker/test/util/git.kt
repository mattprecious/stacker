package com.mattprecious.stacker.test.util

import okio.Path

fun TestEnvironment.gitInit() {
	environment.exec("git init --initial-branch='main'")
	gitSetDefaultBranch("main")
}

fun TestEnvironment.gitAdd(
	vararg files: Path,
) {
	environment.exec("git add ${files.joinToString(" ")}")
}

fun TestEnvironment.gitCommit(
	message: String,
): String {
	// TODO: Escaping.
	environment.exec("git commit --allow-empty -m \"$message\"")
	return gitHeadSha()
}

fun TestEnvironment.gitCheckoutBranch(
	name: String,
) {
	environment.exec("git checkout $name")
}

fun TestEnvironment.gitCreateAndCheckoutBranch(
	name: String,
) {
	environment.exec("git checkout -b $name")
}

fun TestEnvironment.gitSetDefaultBranch(
	name: String,
) {
	environment.exec("git config set init.defaultBranch '$name'")
}

fun TestEnvironment.gitHeadSha(): String {
	return environment.exec("git rev-parse HEAD")
}
