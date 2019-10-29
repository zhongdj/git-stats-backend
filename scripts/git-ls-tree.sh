#!/bin/bash

DIR=$1
echo "git --git-dir=$DIR/.git --work-tree=$DIR ls-tree -r HEAD --name-only"
git --git-dir="$DIR"/.git --work-tree="$DIR" ls-tree -r HEAD --name-only
