package com.mattprecious.stacker.remote.github

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginOauthAccessTokenResponse(
  val access_token: String? = null,
  val token_type: String? = null,
  val scope: String? = null,
  val error: Error? = null,
) {
  @Serializable
  enum class Error {
    @SerialName("authorization_pending") AuthorizationPending,
    @SerialName("slow_down") SlowDown,
    @SerialName("expired_token") ExpiredToken,
    @SerialName("unsupported_grant_type") UnsupportedGrantType,
    @SerialName("incorrect_client_credentials") IncorrectClientCredentials,
    @SerialName("incorrect_device_code") IncorrectDeviceCode,
    @SerialName("access_denied") AccessDenied,
    @SerialName("device_flow_disabled") DeviceFlowDisabled,
  }
}
