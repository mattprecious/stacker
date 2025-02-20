#!/bin/bash
set -e

export CMAKE_BUILD_PARALLEL_LEVEL=$((`nproc`+1))

DEF_PATH=""
BUILD_PATH=""
CMAKE_ARCH=""
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
			SQLITE_ARCH="arm64-apple-macos"
		elif [[ "$RAW_ARCH" == "x86_64" ]]; then
			CMAKE_ARCH="x86_64"
			SQLITE_ARCH="x64-apple-macos"
		else
			echo "Unable to detect Mac architecture."
			exit 1
		fi
	elif [[ $OSTYPE == "linux-gnu"* ]]; then
		if [[ "$RAW_ARCH" == "x86_64" ]]; then
			CMAKE_ARCH="x86_64"
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
  rm -rf libgit2/ sqlite-*/ $BUILD_PATH

  mkdir $BUILD_PATH

  curl -L https://www.sqlite.org/2023/sqlite-autoconf-3440100.tar.gz > sqlite.tar.gz
	tar -xf sqlite.tar.gz
	rm sqlite.tar.gz
	pushd sqlite-*
	CFLAGS="-Os" ./configure --host=$SQLITE_ARCH --prefix=$BUILD_PATH --disable-shared
	make -j$CMAKE_BUILD_PARALLEL_LEVEL
	make -j$CMAKE_BUILD_PARALLEL_LEVEL install
	popd

  curl -L https://github.com/libgit2/libgit2/archive/refs/tags/v1.9.0.zip > libgit2.zip
  tar -xf libgit2.zip
  rm libgit2.zip
  mv libgit2-1.9.0 libgit2
  mkdir -p libgit2/build
  cmake -S libgit2 -B libgit2/build\
    -DUSE_SSH=exec \
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
staticLibraries = libgit2.a libsqlite3.a
libraryPaths = `pwd`/deps/lib
compilerOpts = -I`pwd`/deps/include
linkerOpts = $linkerOpts

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
