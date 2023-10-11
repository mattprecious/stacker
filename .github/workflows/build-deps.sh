#!/bin/bash
set -e

export CMAKE_BUILD_PARALLEL_LEVEL=$((`nproc`+1))

OS_ARCH=""
CMAKE_ARCH=""
OPENSSL_ARCH=""

function usage {
  cat << EOF
  Usage ./build.sh <opts>
  Build needs to be run in root project directory.

  -h    Usage

  -a    Chip architecture
        Options:
          - aarch64
          - amd64
          - x86_64
        Example: -a aarch64

  -c    cmake architecture
        Options:
          - arm64
          - x86_64
        Example: -c arm64

  -o    openssl architecture
        Example: -o darwin64-arm64-cc
EOF
  exit 0
}

function autoDetect() {
	RAW_ARCH=$(uname -m)
	if [[ $OSTYPE == "darwin"* ]]; then
		if [[ "$RAW_ARCH" == "arm64" ]]; then
			OS_ARCH="aarch64"
			CMAKE_ARCH="arm64"
			OPENSSL_ARCH="darwin64-arm64-cc"
		elif [[ "$RAW_ARCH" == "x86_64" ]]; then
			OS_ARCH="x86_64"
			CMAKE_ARCH="x86_64"
			OPENSSL_ARCH="darwin64-x86_64-cc"
		else
			echo "Unable to detect Mac architecture."
			exit 1
		fi
	elif [[ $OSTYPE == "linux-gnu"* ]]; then
		if [[ "$RAW_ARCH" == "x86_64" ]]; then
			OS_ARCH="amd64"
			CMAKE_ARCH="x86_64"
			OPENSSL_ARCH="linux-x86_64"
		else
			echo "Unable to detect Linux architecture."
			exit 1
		fi
  else
		echo "Unable to detect OS."
		exit 1
  fi
}

function build() {
  echo "OS_ARCH=${OS_ARCH}"
  echo "CMAKE_ARCH=${CMAKE_ARCH}"
  echo "OPENSSL_ARCH=${OPENSSL_ARCH}"
  echo ""

  set -x

  # Clean the directories to prevent confusing failure cases
  rm -rf libssh2/ libgit2/ openssl/ deps/

  mkdir deps
  deps="`pwd`/deps"

  git clone --depth 1 --branch openssl-3.1.3 https://github.com/openssl/openssl.git
  pushd openssl
  ./Configure $OPENSSL_ARCH --prefix=$deps no-tests no-legacy
  make -j$CMAKE_BUILD_PARALLEL_LEVEL
  make -j$CMAKE_BUILD_PARALLEL_LEVEL install_sw
  popd

  git clone --depth 1 --branch libssh2-1.11.0 https://github.com/libssh2/libssh2.git
  mkdir -p libssh2/build
  cmake -S libssh2 -B libssh2/build \
    -DCMAKE_PREFIX_PATH="$deps;$deps/include/openssl" \
    -DCMAKE_INSTALL_PREFIX="$deps" \
    -DCMAKE_IGNORE_PREFIX_PATH="/usr" \
    -DCMAKE_OSX_ARCHITECTURES=$CMAKE_ARCH \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_STATIC_LIBS=OFF
  cmake --build libssh2/build --target install

  # Stuck on 1.4.6 due to https://github.com/libgit2/libgit2/issues/6371
  git clone --depth 1 --branch v1.4.6 https://github.com/libgit2/libgit2.git
  mkdir -p libgit2/build
  cmake -S libgit2 -B libgit2/build\
    -DUSE_SSH=ON \
    -DBUILD_TESTS=OFF \
    -DCMAKE_PREFIX_PATH="$deps;$deps/include/openssl" \
    -DCMAKE_INSTALL_PREFIX="$deps" \
    -DCMAKE_IGNORE_PREFIX_PATH="/usr" \
    -DCMAKE_OSX_ARCHITECTURES=$CMAKE_ARCH \
    -DCMAKE_BUILD_TYPE=Release
  cmake --build libgit2/build --target install

  mkdir -p native/$OS_ARCH/
  cp -vL $deps/{lib,lib64}/libcrypto.{dylib,so} native/$OS_ARCH/ || true
  cp -vL $deps/{lib,lib64}/libssl.{dylib,so} native/$OS_ARCH/ || true
  cp -vL $deps/{lib,lib64}/libssh2.{dylib,so} native/$OS_ARCH/ || true
  cp -vL $deps/lib/libgit2.{dylib,so} native/$OS_ARCH/ || true

  echo "Build complete."
  exit 0
}

function processArguments {
  while getopts "h?a:c:o:" opt; do
    case "$opt" in
    h|\?)
        usage
        exit 0
        ;;
    a)
        OS_ARCH=${OPTARG}
        ;;

    c)
        CMAKE_ARCH=${OPTARG}
        ;;

    o)
        OPENSSL_ARCH=${OPTARG}
        ;;
    esac
  done

  shift $((OPTIND-1))
}

# main
autoDetect
processArguments "$@"
build

exit 1
