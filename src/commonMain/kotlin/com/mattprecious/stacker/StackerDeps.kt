package com.mattprecious.stacker

import com.mattprecious.stacker.config.ConfigManager
import com.mattprecious.stacker.lock.Locker
import com.mattprecious.stacker.remote.Remote
import com.mattprecious.stacker.stack.StackManager
import com.mattprecious.stacker.vc.VersionControl

// TODO: DI framework.
class StackerDeps(
  val configManager: ConfigManager,
  val locker: Locker,
  val remote: Remote,
  val stackManager: StackManager,
  val useFancySymbols: Boolean,
  val vc: VersionControl,
)
