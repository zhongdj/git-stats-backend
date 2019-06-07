#!/usr/bin/env bash

REPO_DIR=$1
set -x
git --git-dir="${REPO_DIR}"/.git --work-tree="${REPO_DIR}" rebase
git --git-dir="${REPO_DIR}"/.git --work-tree="${REPO_DIR}" reset --hard HEAD~
git --git-dir="${REPO_DIR}"/.git --work-tree="${REPO_DIR}" pull
