#!/usr/bin/env bash

REPO_DIR=$1
BRANCH=$2
set -x
git --git-dir="${REPO_DIR}"/.git --work-tree="${REPO_DIR}" checkout $BRANCH
