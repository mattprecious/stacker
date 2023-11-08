#!/bin/bash
set -e

export CMAKE_BUILD_PARALLEL_LEVEL=$((`nproc`+1))

CMAKE_ARCH=""
OPENSSL_ARCH=""
SQLITE_ARCH=""

function usage {
  cat << EOF
  Usage ./build.sh <opts>
  Build needs to be run in root project directory.

  -h    Usage

  -c    cmake architecture
        Options:
          - arm64
          - x86_64
        Example: -c arm64

  -o    openssl architecture
        Example: -o darwin64-arm64-cc

  -s    sqlite architecture
        Example: -s arm64-apple-macos
EOF
  exit 0
}

function autoDetect() {
	RAW_ARCH=$(uname -m)
	if [[ $OSTYPE == "darwin"* ]]; then
		if [[ "$RAW_ARCH" == "arm64" ]]; then
			CMAKE_ARCH="arm64"
			OPENSSL_ARCH="darwin64-arm64-cc"
			SQLITE_ARCH="arm64-apple-macos"
		elif [[ "$RAW_ARCH" == "x86_64" ]]; then
			CMAKE_ARCH="x86_64"
			OPENSSL_ARCH="darwin64-x86_64-cc"
			SQLITE_ARCH="x64-apple-macos"
		else
			echo "Unable to detect Mac architecture."
			exit 1
		fi
	elif [[ $OSTYPE == "linux-gnu"* ]]; then
		if [[ "$RAW_ARCH" == "x86_64" ]]; then
			CMAKE_ARCH="x86_64"
			OPENSSL_ARCH="linux-x86_64"
			SQLITE_ARCH="x64-"
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
  echo "CMAKE_ARCH=${CMAKE_ARCH}"
  echo "OPENSSL_ARCH=${OPENSSL_ARCH}"
  echo "SQLITE_ARCH=${SQLITE_ARCH}"
  echo ""

  set -x

  # Clean the directories to prevent confusing failure cases
  rm -rf curl/ libssh2/ libgit2/ openssl/ sqlite-*/ deps/

  mkdir deps
  deps="`pwd`/deps"

  curl -L https://www.sqlite.org/2023/sqlite-autoconf-3440100.tar.gz > sqlite.tar.gz
	tar -xf sqlite.tar.gz
	rm sqlite.tar.gz
	pushd sqlite-*
	CFLAGS="-Os" ./configure --host=$SQLITE_ARCH --prefix=$deps --disable-shared
	make -j$CMAKE_BUILD_PARALLEL_LEVEL
	make -j$CMAKE_BUILD_PARALLEL_LEVEL install
	popd

  git clone --depth 1 --branch openssl-3.1.3 https://github.com/openssl/openssl.git
  pushd openssl
  ./Configure $OPENSSL_ARCH --prefix=$deps no-tests no-legacy no-shared
  make -j$CMAKE_BUILD_PARALLEL_LEVEL
  make -j$CMAKE_BUILD_PARALLEL_LEVEL install_sw
  popd

	git clone --depth 1 --branch curl-8_4_0 https://github.com/curl/curl.git
	mkdir -p curl/build
	cmake -S curl -B curl/build \
		-DCMAKE_PREFIX_PATH="$deps" \
		-DCMAKE_INSTALL_PREFIX="$deps" \
		-DCMAKE_IGNORE_PREFIX_PATH="/usr" \
		-DCMAKE_OSX_ARCHITECTURES=$CMAKE_ARCH \
		-DCMAKE_BUILD_TYPE=Release \
		-DBUILD_SHARED_LIBS=OFF \
		-DCURL_DISABLE_LDAP=ON \
		-DENABLE_IPV6=OFF
	cmake --build curl/build --target install

  git clone --depth 1 --branch libssh2-1.11.0 https://github.com/libssh2/libssh2.git
  mkdir -p libssh2/build
  cmake -S libssh2 -B libssh2/build \
    -DCMAKE_PREFIX_PATH="$deps" \
    -DCMAKE_INSTALL_PREFIX="$deps" \
    -DCRYPTO_BACKEND="OpenSSL" \
    -DCMAKE_IGNORE_PREFIX_PATH="/usr" \
    -DCMAKE_OSX_ARCHITECTURES=$CMAKE_ARCH \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_C_FLAGS="-DOPENSSL_NO_ENGINE" \
    -DBUILD_SHARED_LIBS=OFF
  cmake --build libssh2/build --target install

  # Stuck on 1.4.6 due to https://github.com/libgit2/libgit2/issues/6371
  git clone --depth 1 --branch v1.4.6 https://github.com/libgit2/libgit2.git
  mkdir -p libgit2/build
  cmake -S libgit2 -B libgit2/build\
    -DUSE_SSH=ON \
    -DBUILD_TESTS=OFF \
    -DCMAKE_PREFIX_PATH="$deps" \
    -DCMAKE_INSTALL_PREFIX="$deps" \
    -DCMAKE_IGNORE_PREFIX_PATH="/usr" \
    -DCMAKE_OSX_ARCHITECTURES=$CMAKE_ARCH \
    -DBUILD_SHARED_LIBS=OFF \
    -DCMAKE_BUILD_TYPE=Release
  cmake --build libgit2/build --target install

  linkerOpts="$(PKG_CONFIG_PATH=`pwd`/deps/lib/pkgconfig pkg-config --libs libgit2 --static)"

  mkdir -p src/nativeInterop/cinterop
  cat > src/nativeInterop/cinterop/libgit2.def <<EOF
package = com.github.git2
headers = git2.h
staticLibraries = libgit2.a libsqlite3.a libcurl.a
libraryPaths = `pwd`/deps/lib
compilerOpts = -I`pwd`/deps/include
linkerOpts = $linkerOpts -lcrypto -lssl -lcurl

---

typedef struct git_annotated_commit {} git_annotated_commit;
typedef struct git_branch_iterator {} git_branch_iterator;
typedef struct git_commit {} git_commit;
typedef struct git_config {} git_config;
typedef struct git_object {} git_object;
typedef struct git_reference {} git_reference;
typedef struct git_rebase {} git_rebase;
typedef struct git_remote {} git_remote;
typedef struct git_repository {} git_repository;
EOF

	echo "Done."
  exit 0
}

function processArguments {
  while getopts "h?c:o:s:" opt; do
    case "$opt" in
    h|\?)
        usage
        exit 0
        ;;

    c)
        CMAKE_ARCH=${OPTARG}
        ;;

    o)
        OPENSSL_ARCH=${OPTARG}
        ;;

    s)
        SQLITE_ARCH=${OPTARG}
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
