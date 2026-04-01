package com.mattprecious.stacker.remote.github

import kotlinx.serialization.Serializable

@Serializable
data class LoginDeviceCodeResponse(
  val device_code: String,
  val user_code: String,
  val verification_uri: String,
  val expires_in: Long,
  val interval: Long,
)
