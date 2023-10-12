# Stacker

## Development

### IntelliJ

IntelliJ needs to be configured to opt in to the Java 20 preview language features used in this project. To do this:

1. Open the `Project` window (`⌘1`).
2. Select the root module and open the module settings (`⌘↓`, or right click and select `Module Settings`).
3. `Project Settings` / `Project`.
4. Set the `Language level` to `20 (Preview)`.

### Building Native Libraries

This project uses [libgit2](https://github.com/libgit2/libgit2), which needs to be downloaded and built locally before
the project will compile. This can be done by executing `.github/workflows/build-deps.sh`.

### Generating Bindings

The libgit2 bindings are generated using [jextract](https://github.com/openjdk/jextract) and are built upon the Foreign Function & Memory API available
as a preview in JDK 20. If libgit2 is updated, the bindings need to be regenerated and checked in.

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
  -I libgit2/include/ \
  libgit2/include/git2.h

pushd classes && zip -r ../libgit2j.jar . && popd

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
