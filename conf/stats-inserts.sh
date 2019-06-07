#!/usr/bin/env bash
DIR=$1
FROM=$2
TO=$3

echo $DIR $FROM $TO
cd $DIR

#git fame --format=csv --hide-progressbar --whitespace --after=$FROM --before=$TO --by-type .

git --git-dir="$DIR/.git"  --work-tree="$DIR" log --before="'$TO 23:59:59'" --after="'$FROM 00:00:01'" --shortstat --pretty="%cE" \
| sed 's/\(.*\)@.*/\1/' \
| grep -v "^$" \
| awk 'BEGIN { line=""; } !/^ / { if (line=="" || !match(line, $0)) {line = $0 "," line }} /^ / { print line " # " $0; line=""}' \
| sort \
| sed -E 's/# //;s/ files? changed,//;s/([0-9]+) ([0-9]+ deletion)/\1 0 insertions\(+\), \2/;s/\(\+\)$/\(\+\), 0 deletions\(-\)/;s/insertions?\(\+\), //;s/ deletions?\(-\)//' | awk 'BEGIN {name=""; files=0; insertions=0; deletions=0;} {if ($1 != name && name != "") { print name ": " files " files changed, " insertions " insertions(+), " deletions " deletions(-), " insertions-deletions " net"; files=0; insertions=0; deletions=0; name=$1; } name=$1; files+=$2; insertions+=$3; deletions+=$4} END {print name ": " files " files changed, " insertions " insertions(+), " deletions " deletions(-), " insertions-deletions " net";}'

