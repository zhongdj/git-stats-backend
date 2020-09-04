#!/usr/bin/env bash

REPO_DIR=$1
BRANCH=$2
set -x
git --git-dir="${REPO_DIR}"/.git --work-tree="${REPO_DIR}" checkout master
git --git-dir="${REPO_DIR}"/.git --work-tree="${REPO_DIR}" rebase
git --git-dir="${REPO_DIR}"/.git --work-tree="${REPO_DIR}" reset --hard HEAD~
git --git-dir="${REPO_DIR}"/.git --work-tree="${REPO_DIR}" pull
git --git-dir="${REPO_DIR}"/.git --work-tree="${REPO_DIR}" checkout $BRANCH
