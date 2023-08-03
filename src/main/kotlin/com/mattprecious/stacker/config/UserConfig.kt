package com.mattprecious.stacker.config

import kotlinx.serialization.Serializable

@Serializable
data class UserConfig(
	val githubToken: String?,
)
