#!/bin/bash

DIR=$1
git --git-dir="$DIR"/.git --work-tree="$DIR" ls-tree -r HEAD --name-only
