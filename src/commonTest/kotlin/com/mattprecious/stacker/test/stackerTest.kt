package com.mattprecious.stacker.test

import com.mattprecious.stacker.remote.FakeRemote
import com.mattprecious.stacker.withStacker

fun stackerTest(
	validate: StackerTestScope.() -> Unit,
) {
	StackerTestScope().validate()
}

class StackerTestScope {
	val remote = FakeRemote()

	fun run(vararg args: String) {
		withStacker(remote) { it.parse(args.asList()) }
	}
}
