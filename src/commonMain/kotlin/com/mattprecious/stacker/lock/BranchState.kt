package com.mattprecious.stacker.lock

import kotlinx.serialization.Serializable

@Serializable data class BranchState(val name: String, val sha: String)
