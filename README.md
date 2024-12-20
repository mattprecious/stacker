# Stacker
This project is currently **experimental** and undergoing frequent infrastructure/API changes, thus is not ready for
general usage. If you're feeling adventurous, you're welcome to build the project locally, but please familiarize
yourself with the current list of [issues](https://github.com/mattprecious/stacker/issues) and
[git-reflog](https://git-scm.com/docs/git-reflog).

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
