package com.mattprecious.stacker.vc

class Branch internal constructor(
	private val vc: VersionControl,
	val name: String,
) {
	val metadata: BranchData? = vc.getMetadata(branchName = name)

	val tracked = metadata != null

	val isTrunk = metadata?.isTrunk ?: false

	val parent: Branch? by lazy {
		metadata?.parentName?.let { Branch(vc = vc, name = it) }
	}

	val children: List<Branch> by lazy {
		metadata?.children?.map { Branch(vc = vc, name = it) } ?: emptyList()
	}
}
