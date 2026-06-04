# Stacker
This project is currently **experimental** and undergoing frequent infrastructure/API changes, thus is not ready for
general usage. If you're feeling adventurous, you're welcome to build the project locally, but please familiarize
yourself with the current list of [issues](https://github.com/mattprecious/stacker/issues) and
[git-reflog](https://git-scm.com/docs/git-reflog).

## Overview

Stacker (`st`) is a command-line tool for working with **stacked branches** — small, dependent branches layered on top
of one another, each with its own pull request. Instead of putting a large change into a single branch and PR, you split
it into a stack of focused branches, keep building without waiting for review, and let Stacker keep the stack in order.

Stacker maintains its own record of the parent/child relationship between branches, forming a tree rooted at your
**trunk** (e.g. `main`). When a branch low in the stack changes — because you amended it, or it merged — Stacker
restacks the branches above it by rebasing them in the correct order. A **trailing-trunk** workflow is also supported,
where you branch off a separate base branch while still targeting trunk with your pull requests.

Pull requests are created and updated directly through the GitHub API (authenticated via OAuth device code); Stacker
does not shell out to the `gh` CLI.

## Commands

Run `st <command> --help` for the full set of options on any command. Most commands have a short alias, and aliases
compose down the command tree, so `st branch checkout` can also be written `st b co` or simply `st bco`.

### `repo` (`r`) — repository setup and maintenance
* `init` — interactively select your trunk branch (and, optionally, a trailing trunk). Run this once before using any
  other command.
* `sync` (`s`) — pull trunk, then walk the tracked branches and offer to delete any whose pull request has been merged
  or closed.

### `branch` (`b`) — everyday branch operations
* `create` (`c`) — create a new branch on top of the current one and start tracking it.
* `track` (`tr`) / `untrack` (`ut`) — start or stop tracking an existing branch in the stack.
* `checkout` (`co`) — check out a branch, chosen interactively from the stack or passed by name.
* `up` (`u`) / `down` (`d`) — move one branch up or down the stack.
* `top` (`t`) / `bottom` (`b`) — jump to the top or bottom of the current stack.
* `rename` (`rn`) / `delete` (`dl`) — rename or delete a tracked branch.
* `restack` (`r`) — rebase the current branch onto its parent.
* `submit` (`s`) — push the current branch and open or update its pull request.

### `upstack` (`us`) — the current branch and everything above it
* `onto` (`o`) — re-parent the current branch onto a different branch, then restack everything above it.
* `restack` (`r`) — restack the current branch and all of its descendants.

### `downstack` (`ds`) — the current branch and everything below it
* `edit` (`e`) — interactively reorder, untrack, remove, or delete branches in the current stack (down to
  trunk), then re-parent and restack them.

### `stack` (`s`) — the entire stack
* `submit` (`s`) — push every branch in the current stack and open or update all of their pull requests.

### `log` (`l`) — visualize the stack
* `short` (`s`) — print the stack as a tree, marking any branches that need a restack.

### `rebase` — recover from a restack that hit conflicts
* `--continue` — resume the in-progress restack after resolving conflicts.
* `--abort` — abort the in-progress restack.

## Development

### Building

The generic `assemble` and `build` tasks will attempt to build for all supported architectures, which cannot be done
locally. Instead, run the gradle task for your current architecture:

* `./gradlew linkReleaseExecutableMacosArm`
* `./gradlew linkReleaseExecutableMacosX64`

Debug variants are available by replacing `Release` with `Debug`. Other architectures are not currently supported.

### Running

After building, an executable will be available in the `build` folder. Its exact path will depend on the task that was
used to build it. For reference, a release macOS ARM build will be available at
`build/bin/macosArm64/debugExecutable/stacker.kexe`.

# License

    Copyright 2023 Matthew Precious

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
