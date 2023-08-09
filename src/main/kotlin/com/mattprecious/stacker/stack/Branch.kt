package com.mattprecious.stacker.stack

interface Branch {
	val name: String
	val parent: Branch?
	val children: List<Branch>
}
