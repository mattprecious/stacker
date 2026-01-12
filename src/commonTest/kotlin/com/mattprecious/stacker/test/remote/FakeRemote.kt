package com.mattprecious.stacker.remote

class FakeRemote : Remote {
  override val isAuthenticated: Boolean
    get() = TODO("Not yet implemented")

  override val repoName: String?
    get() = TODO("Not yet implemented")

  override val hasRepoAccess: Boolean
    get() = TODO("Not yet implemented")

  override fun setToken(token: String): Boolean {
    TODO("Not yet implemented")
  }

  override fun getPrStatus(branchName: String): Remote.PrStatus {
    TODO("Not yet implemented")
  }

  override fun openOrRetargetPullRequest(
    branchName: String,
    targetName: String,
    prInfo: () -> Remote.PrInfo,
  ): Remote.PrResult {
    TODO("Not yet implemented")
  }
}
