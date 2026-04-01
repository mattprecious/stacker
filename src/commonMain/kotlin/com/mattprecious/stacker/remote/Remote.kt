package com.mattprecious.stacker.remote

import kotlinx.coroutines.flow.Flow

interface Remote {
  val isAuthenticated: Boolean
  val repoName: String?
  val hasRepoAccess: Boolean

  fun requestAccessCode(): Flow<AccessCodeState>

  fun getPrStatus(branchName: String): PrStatus

  fun openOrRetargetPullRequest(
    branchName: String,
    targetName: String,
    prInfo: () -> PrInfo,
  ): PrResult

  sealed interface AccessCodeState {
    data object Requesting : AccessCodeState

    data class WaitingForApproval(val verificationUrl: String, val userCode: String) :
      AccessCodeState

    data object Denied : AccessCodeState

    data object TimedOut : AccessCodeState

    data object Finished : AccessCodeState
  }

  data class PrInfo(val title: String, val body: String?)

  enum class PrStatus {
    NotFound,
    Open,
    Closed,
    Merged,
  }

  sealed interface PrResult {
    val url: String
    val number: Long

    data class Created(override val url: String, override val number: Long) : PrResult

    data class Updated(override val url: String, override val number: Long) : PrResult

    data class NoChange(override val url: String, override val number: Long) : PrResult
  }
}
