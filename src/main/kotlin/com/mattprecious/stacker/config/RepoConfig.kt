package com.mattprecious.stacker.config

import kotlinx.serialization.Serializable

@Serializable
data class RepoConfig(
	val trunk: String,
	val trailingTrunk: String?,
)
