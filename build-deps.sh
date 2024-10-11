#!/bin/bash
set -e

export CMAKE_BUILD_PARALLEL_LEVEL=$((`nproc`+1))

DEF_PATH=""
BUILD_PATH=""
CMAKE_ARCH=""
OPENSSL_ARCH=""
SQLITE_ARCH=""

function usage {
  cat << EOF
Usage ./build-deps.sh <opts>
Build needs to be run in root project directory.

-h    Usage

-d    def file path
      Example: -d src/nativeInterop/cinterop/libgit2.def

-b    build output directory
      Example: -b deps
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
  echo "DEF_PATH=${DEF_PATH}"
  echo "BUILD_PATH=${BUILD_PATH}"
  echo "CMAKE_ARCH=${CMAKE_ARCH}"
  echo "OPENSSL_ARCH=${OPENSSL_ARCH}"
  echo "SQLITE_ARCH=${SQLITE_ARCH}"
  echo ""

  if [[ $DEF_PATH == "" ]]; then
		echo "Def file path must be specified."
		echo ""
		usage
		exit 1
	elif [[ $BUILD_PATH = "" ]]; then
		echo "Build output directory must be specified."
		echo ""
		usage
		exit 1
	fi

  set -x

  # Clean the directories to prevent confusing failure cases
  rm -rf curl/ libssh2/ libgit2/ openssl/ sqlite-*/ $BUILD_PATH

  mkdir $BUILD_PATH

  curl -L https://www.sqlite.org/2023/sqlite-autoconf-3440100.tar.gz > sqlite.tar.gz
	tar -xf sqlite.tar.gz
	rm sqlite.tar.gz
	pushd sqlite-*
	CFLAGS="-Os" ./configure --host=$SQLITE_ARCH --prefix=$BUILD_PATH --disable-shared
	make -j$CMAKE_BUILD_PARALLEL_LEVEL
	make -j$CMAKE_BUILD_PARALLEL_LEVEL install
	popd

  curl -L https://github.com/openssl/openssl/archive/refs/tags/openssl-3.1.3.zip > openssl.zip
  tar -xf openssl.zip
  rm openssl.zip
  mv openssl-openssl-3.1.3 openssl
  pushd openssl
  ./Configure $OPENSSL_ARCH --prefix=$BUILD_PATH no-tests no-legacy no-shared
  make -j$CMAKE_BUILD_PARALLEL_LEVEL
  make -j$CMAKE_BUILD_PARALLEL_LEVEL install_sw
  popd

  curl -L https://github.com/curl/curl/archive/refs/tags/curl-8_4_0.zip > curl.zip
  tar -xf curl.zip
  rm curl.zip
  mv curl-curl-8_4_0 curl
	mkdir -p curl/build
	cmake -S curl -B curl/build \
		-DCMAKE_PREFIX_PATH="$BUILD_PATH" \
		-DCMAKE_INSTALL_PREFIX="$BUILD_PATH" \
		-DCMAKE_IGNORE_PREFIX_PATH="/usr" \
		-DCMAKE_OSX_ARCHITECTURES=$CMAKE_ARCH \
		-DCMAKE_BUILD_TYPE=Release \
		-DBUILD_SHARED_LIBS=OFF \
		-DCURL_DISABLE_LDAP=ON \
		-DENABLE_IPV6=OFF
	cmake --build curl/build --target install

  curl -L https://github.com/libssh2/libssh2/archive/refs/tags/libssh2-1.11.0.zip > libssh2.zip
  tar -xf libssh2.zip
  rm libssh2.zip
  mv libssh2-libssh2-1.11.0 libssh2
  mkdir -p libssh2/build
  cmake -S libssh2 -B libssh2/build \
    -DCMAKE_PREFIX_PATH="$BUILD_PATH" \
    -DCMAKE_INSTALL_PREFIX="$BUILD_PATH" \
    -DCRYPTO_BACKEND="OpenSSL" \
    -DCMAKE_IGNORE_PREFIX_PATH="/usr" \
    -DCMAKE_OSX_ARCHITECTURES=$CMAKE_ARCH \
    -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_C_FLAGS="-DOPENSSL_NO_ENGINE" \
    -DBUILD_SHARED_LIBS=OFF
  cmake --build libssh2/build --target install

  # Stuck on 1.4.6 due to https://github.com/libgit2/libgit2/issues/6371
  curl -L https://github.com/libgit2/libgit2/archive/refs/tags/v1.4.6.zip > libgit2.zip
  tar -xf libgit2.zip
  rm libgit2.zip
  mv libgit2-1.4.6 libgit2
  mkdir -p libgit2/build
  cmake -S libgit2 -B libgit2/build\
    -DUSE_SSH=ON \
    -DBUILD_TESTS=OFF \
    -DCMAKE_PREFIX_PATH="$BUILD_PATH" \
    -DCMAKE_INSTALL_PREFIX="$BUILD_PATH" \
    -DCMAKE_IGNORE_PREFIX_PATH="/usr" \
    -DCMAKE_OSX_ARCHITECTURES=$CMAKE_ARCH \
    -DBUILD_SHARED_LIBS=OFF \
    -DCMAKE_BUILD_TYPE=Release
  cmake --build libgit2/build --target install

  linkerOpts="$(PKG_CONFIG_PATH=`pwd`/deps/lib/pkgconfig pkg-config --libs libgit2 --static)"

  mkdir -p `dirname $DEF_PATH`
  cat > $DEF_PATH <<EOF
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
  while getopts "h?d:b:" opt; do
    case "$opt" in
    h|\?)
        usage
        exit 0
        ;;

    d)
        DEF_PATH=${OPTARG}
        ;;

    b)
        BUILD_PATH=${OPTARG}
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
