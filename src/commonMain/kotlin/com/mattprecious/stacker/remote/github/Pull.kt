package com.mattprecious.stacker.remote.github

import kotlinx.serialization.Serializable

@Serializable
data class Pull(
  val number: Long,
  val merged_at: String?,
  val state: State,
  val html_url: String,
  val base: Base,
) {
  @Serializable
  enum class State {
    Open,
    Closed,
  }

  @Serializable data class Base(val ref: String)
}
