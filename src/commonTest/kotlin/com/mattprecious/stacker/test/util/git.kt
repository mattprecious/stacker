package com.mattprecious.stacker.test.util

import okio.Path

value class Sha(val long: String) {
	val short: String
		get() = long.substring(0, 7)
}

fun TestEnvironment.gitInit() {
	environment.exec("git init --initial-branch='main'")
	gitSetDefaultBranch("main")
}

fun TestEnvironment.gitAdd(
	vararg files: Path,
) {
	environment.exec("git add ${files.joinToString(" ")}")
}

fun TestEnvironment.gitBranches(): Sequence<String> {
	return environment.exec("git branch").splitToSequence('\n').map { it.trim() }
}

fun TestEnvironment.gitCommit(
	message: String,
): Sha {
	// TODO: Escaping.
	environment.exec("git commit --allow-empty -m \"$message\"")
	return gitSha()
}

fun TestEnvironment.gitCheckoutBranch(
	name: String,
) {
	environment.exec("git checkout $name")
}

fun TestEnvironment.gitCreateBranch(
	name: String,
	startPoint: String = "HEAD",
) {
	environment.exec("git branch $name $startPoint")
}

fun TestEnvironment.gitCreateAndCheckoutBranch(
	name: String,
) {
	environment.exec("git checkout -b $name")
}

fun TestEnvironment.gitLog(path: String = "HEAD"): Sequence<String> {
	return environment.exec("git log --format=format:'%h %s' $path").splitToSequence('\n')
}

fun TestEnvironment.gitSetDefaultBranch(
	name: String,
) {
	environment.exec("git config set init.defaultBranch '$name'")
}

fun TestEnvironment.gitSha(rev: String = "HEAD"): Sha {
	return Sha(environment.exec("git rev-parse $rev"))
}
