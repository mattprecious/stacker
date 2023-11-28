package com.mattprecious.stacker.stack

interface Branch {
	val name: String
	val parent: Branch?
	val parentSha: String?
	val children: List<Branch>
}
