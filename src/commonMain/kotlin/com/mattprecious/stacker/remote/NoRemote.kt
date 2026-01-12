package com.mattprecious.stacker.remote

class NoRemote : Remote {
  override val isAuthenticated: Boolean
    get() = throw NotImplementedError()

  override val repoName: String?
    get() = throw NotImplementedError()

  override val hasRepoAccess: Boolean
    get() = throw NotImplementedError()

  override fun setToken(token: String): Boolean {
    throw NotImplementedError()
  }

  override fun getPrStatus(branchName: String): Remote.PrStatus {
    throw NotImplementedError()
  }

  override fun openOrRetargetPullRequest(
    branchName: String,
    targetName: String,
    prInfo: () -> Remote.PrInfo,
  ): Remote.PrResult {
    throw NotImplementedError()
  }
}
