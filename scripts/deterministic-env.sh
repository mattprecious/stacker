#!/bin/bash

FOLDER="$TMPDIR/stackerEnv-$(date +%Y%m%d%H%M%S)"
mkdir $FOLDER
pushd $FOLDER > /dev/null

GIT_AUTHOR_DATE="2020-01-01T12:00:00Z" \
GIT_COMMITTER_DATE="2020-01-01T12:00:00Z" \
GIT_AUTHOR_EMAIL="stacker@example.com" \
GIT_COMMITTER_EMAIL="stacker@example.com" \
GIT_AUTHOR_NAME="Stacker" \
GIT_COMMITTER_NAME="Stacker" \
env $SHELL

popd > /dev/null
rm -rf $FOLDER
