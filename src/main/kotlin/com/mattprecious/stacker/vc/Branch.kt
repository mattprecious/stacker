package com.mattprecious.stacker.vc

class Branch internal constructor(
	private val vc: VersionControl,
	val name: String,
) {
	private val metadata: BranchData? = vc.getMetadata(branchName = name)

	val tracked = metadata != null

	val isTrunk = metadata?.isTrunk ?: false

	val parent: Branch? by lazy {
		metadata?.parentName?.let { Branch(vc = vc, name = it) }
	}
}
