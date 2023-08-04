package com.mattprecious.stacker.vc

import kotlinx.serialization.Serializable

@Serializable
data class BranchData(
	val isTrunk: Boolean,
	val parentName: String?,
	val children: List<String>,
)
