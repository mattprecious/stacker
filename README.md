# Stacker

# Stacker

## libgit2

### libssh
`libssh` is required in order to build `libgit2` with SSH and SSH transport support (for repositories with SSH remotes).
Install it with:

```sh
brew install libssh
```

### Building Library
Clone and build `libgit2` with:

```sh
git clone git@github.com:libgit2/libgit2.git
cd libgit2/build
cmake -DUSE_SSH=ON ..
cmake --build .
```

This will create a `libgit2.1.8.0.dylib` which can be copied into the project at `resources/jni/aarch64/libgit2.dynlib`.

_TODO: Build for more architectures._

### Generating Bindings

The bindings are generated using [jextract](https://github.com/openjdk/jextract) and are built upon the Foreign Function
& Memory API available as a preview in JDK 20.

Install JDK 20:

```sh
brew install zulu-jdk20
```

Download jextract from [here](https://jdk.java.net/jextract/) and optionally add it to your PATH.

Generate the bindings and bundle them into a JAR:

```sh
mkdir libgit2-bindings && cd libgit2-bindings

jextract --output classes -t com.github \
  -I /Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/usr/include/ \
  -I <libgit2folder>/include/ \
  -I <libgit2folder>/include/git2 \
  <libgit2folder>/include/git2.h

cd classes && zip -r ../libgit2j.jar . && popd
```

This JAR can be copied into the project at `libs/libgit2j.jar`.

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
