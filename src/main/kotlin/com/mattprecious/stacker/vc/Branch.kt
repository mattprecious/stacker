package com.mattprecious.stacker.vc

class Branch internal constructor(
	private val vc: VersionControl,
	val name: String,
) {
	private var metadata: BranchData?
		get() = vc.getMetadata(branchName = name)
		set(value) {
			vc.setMetadata(name, value)
		}

	val tracked = metadata != null

	val isTrunk = metadata?.isTrunk ?: false

	var parent: Branch?
		get() {
			return metadata?.parentName?.let { Branch(vc = vc, name = it) }
		}
		private set(value) {
			val data = requireNotNull(metadata) {
				"Cannot set parent of an untracked branch."
			}

			parent?.removeChild(this)
			metadata = data.copy(parentName = value?.name)
			value?.addChild(this)
		}

	val children: List<Branch> by lazy {
		metadata?.children?.map { Branch(vc = vc, name = it) } ?: emptyList()
	}

	fun track(
		parent: Branch?,
		isTrunk: Boolean = false,
	) {
		require(!tracked) {
			"Already tracked."
		}

		vc.track(
			branch = this,
			isTrunk = isTrunk,
		)

		this.parent = parent
	}

	fun untrack() {
		require(children.isEmpty()) {
			"Branch has children: $children."
		}

		parent?.removeChild(this)
		vc.untrack(this)
	}

	override fun toString(): String {
		return name
	}

	private fun addChild(branch: Branch) {
		val data = requireNotNull(metadata) {
			"Branch is not tracked."
		}

		metadata = data.copy(children = data.children + branch.name)
	}

	private fun removeChild(branch: Branch) {
		val data = requireNotNull(metadata) {
			"Branch is not tracked."
		}

		metadata = data.copy(children = data.children - branch.name)
	}
}
