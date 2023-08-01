package com.mattprecious.stacker

import kotlinx.serialization.Serializable

@Serializable
data class Config(
	val trunk: String,
	val trailingTrunk: String?,
)
