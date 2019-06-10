#!/bin/bash

# git --git-dir='/Users/barry/Workspaces/external/git-stats-backend/.git' --work-tree='/Users/barry/Workspaces/external/git-stats-backend' log -- '.gitignore'
# Usage git-file-log.sh /Users/barry/Workspaces/external/git-stats-backend .gitignore

DIR=$1
FILE=$2
echo "git --git-dir=$DIR/.git --work-tree=$DIR log --date=format:'%Y-%m-%d %H:%M:%S' -- $2"
git --git-dir="$DIR"/.git --work-tree="$DIR" log --date=format:'%Y-%m-%d %H:%M:%S' -- "$2"