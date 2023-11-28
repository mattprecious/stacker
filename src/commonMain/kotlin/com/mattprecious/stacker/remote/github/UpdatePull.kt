
package com.mattprecious.stacker.remote.github

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePull(
	val base: String,
)
