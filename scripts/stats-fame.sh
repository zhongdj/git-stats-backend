#!/usr/bin/env bash
DIR=$1
FROM=$2
TO=$3

echo $DIR $FROM $TO
cd $DIR

git fame --format=csv --hide-progressbar --whitespace --after=$FROM --before=$TO --by-type .

